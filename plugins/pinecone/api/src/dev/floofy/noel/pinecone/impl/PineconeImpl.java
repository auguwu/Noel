/// 🐾✨ Noel: Discord bot made to manage my servers made in Java
/// Copyright 2021-2026 Noel Towa <cutie@floofy.dev>
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.

package dev.floofy.noel.pinecone.impl;

import com.google.inject.Inject;
import dev.floofy.noel.Pinecone;
import dev.floofy.noel.Setting;
import dev.floofy.noel.Settings;
import dev.floofy.noel.pinecone.annotations.SlashCommand;
import dev.floofy.noel.pinecone.api.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.*;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Pushes registered slash commands to Discord and dispatches interactions to them.
///
/// Pinecone's implementation: syncs commands with Discord, and handles slash
/// command and autocomplete interactions.
///
/// ## Syncing
/// Commands come from the [CommandRegistry]. They are first synced to Discord as
/// soon as both every plugin has enabled ([#markPluginsReady()]) and JDA is ready,
/// in whichever order those happen. After that, [#resync()] pushes them again,
/// for example after the configuration is reloaded.
///
/// A sync replaces Discord's command lists wholesale: one bulk update for the
/// global commands, one per guild that has guild-only commands, and an empty
/// update for any guild that had guild-only commands but no longer does.
/// Discord deletes whatever isn't in a bulk update, so commands that were
/// removed or moved disappear without being deleted one by one.
///
/// ## Interactions
/// Slash commands and autocomplete requests run on a dedicated thread pool, so a
/// slow command never blocks JDA's event thread. Each slash command invocation
/// runs through the [CommandInterceptor] chain, then the [Dispatcher].
public final class PineconeImpl extends ListenerAdapter implements Pinecone, AutoCloseable {
    /// Where a command is registered.
    ///
    /// @param global whether it's a global command
    /// @param guilds the guilds it's registered in, if it isn't global. Empty
    ///               for a non-global command means nowhere, which only a
    ///               [GuildScope] can produce.
    record Placement(boolean global, long[] guilds) {
        static final Placement GLOBAL = new Placement(true, new long[0]);
        static final Placement NOWHERE = new Placement(false, new long[0]);

        static Placement in(long[] guilds) {
            return guilds.length == 0 ? NOWHERE : new Placement(false, guilds);
        }
    }

    private static final String FAIL_MESSAGE =
            ":pensive: **|> command failed to execute. this was reported to the owners!**";

    private static final Logger LOG = LoggerFactory.getLogger(PineconeImpl.class);

    private final List<CommandInterceptor> interceptors = new CopyOnWriteArrayList<>();
    private final AtomicBoolean pluginsReady = new AtomicBoolean();
    private final AtomicBoolean jdaReady = new AtomicBoolean();
    private final AtomicBoolean synced = new AtomicBoolean();

    private final ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger count = new AtomicInteger();

        @Override
        public Thread newThread(@NonNull Runnable runnable) {
            final var thread = new Thread(runnable, "Noel-CommandExecution[%d]".formatted(count.incrementAndGet()));
            thread.setDaemon(true);
            return thread;
        }
    });

    /// Discord's IDs for guild-only commands, by guild, then by name. Each guild
    /// has its own ID for the same command.
    private final Map<Long, Map<String, Long>> guildIds = new ConcurrentHashMap<>();
    private final CommandRegistry registry;
    private final Settings settings;
    private final JDA jda;

    private volatile Map<String, Placement> resolvedPlacements = Map.of();

    /// Discord's IDs for the global commands, by name, as of the last sync.
    private volatile Map<String, Long> globalIds = Map.of();

    @Inject
    PineconeImpl(CommandRegistry registry, Settings settings, JDA jda) {
        this.registry = registry;
        this.settings = settings;
        this.jda = jda;
    }
    /// Works out where a command is registered. The first of these that applies
    /// wins:
    ///
    /// 1. `commands.<name>.onlyInGuilds` in the configuration, so any command can
    ///    be moved without a rebuild. An empty list makes it global.
    /// 2. Its [GuildScope], from `@GuildScoped`, asked now. An empty set means
    ///    nowhere.
    /// 3. [SlashCommand#onlyInGuilds()]. An empty array means global.
    ///
    /// ```yaml
    /// commands:
    ///   eval:
    ///     onlyInGuilds: [824066105102303232, "1001338219591585792"]
    /// ```
    ///
    /// @throws dev.floofy.noel.SettingException if the configured value is not a list of guild IDs
    Placement placementFor(CommandRegistry.Registered registered) {
        final var info = registered.info();
        final var setting =
                Setting.list(String.format("commands['%s'].onlyInGuilds".formatted(info.name())), Setting::snowflake);

        final var configured = settings.get(setting);
        if (configured.isPresent()) {
            final long[] guilds =
                    configured.get().stream().mapToLong(Long::longValue).toArray();
            return guilds.length == 0 ? Placement.GLOBAL : Placement.in(guilds);
        }

        final var scope = registered.scope();
        if (scope != null) {
            try {
                return Placement.in(
                        scope.guilds().stream().mapToLong(Long::longValue).toArray());
            } catch (RuntimeException ex) {
                LOG.error("guild scope of /{} failed; registering it nowhere until the next sync", info.name(), ex);
                return Placement.NOWHERE;
            }
        }

        return info.onlyInGuilds().length == 0 ? Placement.GLOBAL : Placement.in(info.onlyInGuilds());
    }

    @Override
    public void onReady(@NonNull ReadyEvent event) {
        jdaReady.set(true);
        trySync();
    }

    /// Signals that every plugin has enabled, so the registry is complete.
    public void markPluginsReady() {
        pluginsReady.set(true);
        if (jda.getStatus() == JDA.Status.CONNECTED) {
            jdaReady.set(true);
        }

        trySync();
    }

    private void trySync() {
        if (jdaReady.get() && pluginsReady.get() && synced.compareAndSet(false, true)) {
            sync().whenComplete(PineconeImpl::logSyncResult);
        }
    }

    @Override
    public CompletableFuture<Void> resync() {
        if (!synced.get()) {
            LOG.info("skipping resync: the initial sync hasn't happened yet");
            return CompletableFuture.completedFuture(null);
        }

        final CompletableFuture<Void> result = sync();
        result.whenComplete(PineconeImpl::logSyncResult);
        return result;
    }

    private static void logSyncResult(Void ignored, Throwable error) {
        if (error != null) {
            LOG.error("failed to sync slash commands with Discord", error);
        } else {
            LOG.info("slash commands are in sync with Discord");
        }
    }

    private synchronized CompletableFuture<Void> sync() {
        try {
            return doSync();
        } catch (RuntimeException ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    private CompletableFuture<Void> doSync() {
        final var registered = registry.all();
        LOG.info("Preparing to register {} slash commands", registered.size());

        CommandListUpdateAction globalCommands = jda.updateCommands();
        final HashMap<Long, CommandListUpdateAction> guildOnlySlashCommands = new HashMap<>();
        final HashMap<String, Placement> placements = new HashMap<>();
        for (var entry : registered) {
            placements.put(entry.info().name(), placementFor(entry));
        }

        this.resolvedPlacements = Map.copyOf(placements);
        for (var ent : registered) {
            final var command = ent.prototype();
            final var info = command.getInfo();
            final var placement = placements.get(info.name());
            if (!placement.global() && placement.guilds().length == 0) {
                LOG.warn("Not registering /{}: its scope has no guilds", info.name());
                continue;
            }

            final var guilds = placement.guilds();
            SlashCommandData data = Commands.slash(info.name(), info.description());

            if (info.defaultPermissions().length > 0) {
                data = data.setDefaultPermissions(DefaultMemberPermissions.enabledFor(info.defaultPermissions()))
                        .setContexts(InteractionContextType.GUILD);
            }

            final var autocompleted = command.getAutocompleters().keySet();
            attachSubcommands(command, data, autocompleted);
            data.addOptions(optionData(command.getOptions(), autocompleted));

            if (placement.global()) {
                LOG.info("Registering global slash command /{}", info.name());
                globalCommands = globalCommands.addCommands(data);
            } else {
                LOG.info(
                        "Registering guild-only slash command /{} in guilds: [{}]",
                        info.name(),
                        String.join(
                                ", ",
                                Arrays.stream(guilds).mapToObj(Long::toString).toList()));

                for (long id : guilds) {
                    final var guild = jda.getGuildById(id);
                    if (guild == null) {
                        LOG.warn("not registering /{} in guild {}: Noel isn't in it", info.name(), id);
                        continue;
                    }

                    final var update = guildOnlySlashCommands.computeIfAbsent(
                            guild.getIdLong(), ignored -> guild.updateCommands());

                    guildOnlySlashCommands.put(guild.getIdLong(), update.addCommands(data));
                }
            }
        }

        final List<CompletableFuture<?>> updates = new ArrayList<>();
        updates.add(globalCommands.submit().thenAccept(commands -> {
            LOG.info("successfully upserted or updated {} global commands", commands.size());
            this.globalIds = idsByName(commands);
        }));

        for (var ent : guildOnlySlashCommands.entrySet()) {
            updates.add(ent.getValue().submit().thenAccept(cmds -> {
                LOG.info("[{}] successfully upserted or updated {} guild-only commands", ent.getKey(), cmds.size());
                guildIds.put(ent.getKey(), idsByName(cmds));
            }));
        }

        updates.addAll(clearStaleGuilds(guildOnlySlashCommands.keySet()));
        return CompletableFuture.allOf(updates.toArray(CompletableFuture[]::new));
    }

    private static Map<String, Long> idsByName(List<Command> commands) {
        return commands.stream().collect(Collectors.toUnmodifiableMap(Command::getName, Command::getIdLong));
    }

    private List<CompletableFuture<?>> clearStaleGuilds(Set<Long> updatedGuilds) {
        final List<CompletableFuture<?>> out = new ArrayList<>();
        for (var guild : jda.getGuilds()) {
            if (updatedGuilds.contains(guild.getIdLong())) {
                continue;
            }

            out.add(guild.retrieveCommands().submit().thenCompose(commands -> {
                if (commands.isEmpty()) {
                    return CompletableFuture.<Void>completedFuture(null);
                }

                LOG.info(
                        "[{} ({})] removing stale guild-only command(s): {}",
                        guild.getName(),
                        guild.getId(),
                        commands.stream().map(cmd -> "/" + cmd.getName()).toList());

                return guild.updateCommands().submit().thenAccept(ignored -> {});
            }));
        }

        return out;
    }

    @Override
    public void onSlashCommandInteraction(@NonNull SlashCommandInteractionEvent event) {
        LOG.debug(
                "received slash command request :: /{} from {} ({})",
                event.getFullCommandName(),
                event.getUser().getName(),
                event.getUser().getId());

        if (!pluginsReady.get()) {
            event.reply(":hourglass: **|> Bot is still starting up; try again in a moment!**")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        final var command = registry.find(event.getName());
        if (command.isEmpty()) {
            event.replyFormat(":question: **|> unknown slash command: /%s**", event.getName())
                    .setEphemeral(true)
                    .queue();

            return;
        }

        final var registered = command.get();
        final var info = infoOf(registered);
        final var guild = event.getGuild();
        if (guild == null ? !info.isGlobal() : !info.isAvailableIn(guild.getIdLong())) {
            event.reply(":no_entry: **|> `/%s` isn't available here**".formatted(info.name()))
                    .setEphemeral(true)
                    .queue();

            return;
        }

        final var context = new CommandContextImpl(event.getInteraction(), this);
        try {
            executor.execute(() -> invoke(registered, info, context));
        } catch (RejectedExecutionException ex) {
            event.reply(":zzz: **|> bot is restarting; try again in a moment**")
                    .setEphemeral(true)
                    .queue();
        }
    }

    private void invoke(CommandRegistry.Registered registered, CommandInfo info, CommandContext context) {
        Objects.requireNonNull(registered, "registered");
        Objects.requireNonNull(info, "info");
        Objects.requireNonNull(context, "context");

        final var chain = new ArrayList<>(List.copyOf(this.interceptors));
        chain.addAll(registered.interceptors());

        final Duration took;

        try {
            for (var interceptor : chain) {
                if (!interceptor.before(context, info)) {
                    LOG.debug("slash command [/{}] was stopped by interceptor {}", info.name(), interceptor);
                    return;
                }
            }

            final long start = System.nanoTime();
            Dispatcher.dispatch(registered.provider().get(), context);
            took = Duration.ofNanos(System.nanoTime() - start);
        } catch (Throwable ex) {
            if (ex instanceof VirtualMachineError fatal) {
                throw fatal;
            }

            handleError(chain, context, info, ex);
            return;
        }

        for (var interceptor : chain) {
            try {
                interceptor.after(context, info, took);
            } catch (RuntimeException ex) {
                LOG.error("interceptor {} failed in `after()' for /{}:", interceptor, info.name(), ex);
            }
        }
    }

    private void handleError(
            List<CommandInterceptor> chain, CommandContext context, CommandInfo info, Throwable error) {
        boolean handled = false;
        for (var interceptor : chain) {
            try {
                handled |= interceptor.onError(context, info, error);
            } catch (RuntimeException ex) {
                LOG.error("interceptor {} failed in `onError()' for /{}", interceptor, info.name(), ex);
            }
        }

        if (handled) {
            return;
        }

        LOG.error("failed to run slash command /{}:", info.name(), error);
        if (context.getInteraction().isAcknowledged()) {
            context.getInteraction()
                    .getHook()
                    .sendMessage(FAIL_MESSAGE)
                    .setEphemeral(true)
                    .queue();
        } else {
            context.reply(FAIL_MESSAGE).setEphemeral(true).queue();
        }
    }

    @Override
    public AutoCloseable addInterceptor(CommandInterceptor interceptor) {
        Objects.requireNonNull(interceptor, "interceptor");

        interceptors.add(interceptor);
        return () -> interceptors.remove(interceptor);
    }

    @Override
    public Optional<String> mentionSlashCommand(String path, Long guild) {
        final var parts = path.strip().split("\\s+");
        if (parts.length == 0 || parts.length > 3 || parts[0].isEmpty()) {
            throw new IllegalArgumentException(
                    "expected `command`, `command subcommand` or `command group subcommand`, got `%s`".formatted(path));
        }

        Long id = null;
        if (guild != null) {
            id = guildIds.getOrDefault(guild, Map.of()).get(parts[0]);
        }

        if (id == null) {
            id = globalIds.get(parts[0]);
        }

        return id == null ? Optional.empty() : Optional.of("</%s:%d>".formatted(String.join(" ", parts), id));
    }

    private CommandInfo infoOf(CommandRegistry.Registered registered) {
        var placement = resolvedPlacements.get(registered.info().name());
        if (placement == null) {
            final long[] annotated = registered.info().onlyInGuilds();
            placement = registered.scope() != null
                    ? Placement.NOWHERE
                    : annotated.length == 0 ? Placement.GLOBAL : Placement.in(annotated);
        }

        return CommandInfo.from(registered.prototype(), registered.pluginId(), placement.guilds(), placement.global());
    }

    @Override
    public Set<CommandInfo> getSlashCommands() {
        return registry.all().stream().map(this::infoOf).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    void attachSubcommands(AbstractSlashCommand command, SlashCommandData data, Set<String> autocompleted) {
        for (var entry : command.getSubcommands().entrySet()) {
            data.addSubcommands(subcommandData(entry.getKey(), entry.getValue(), autocompleted));
        }

        for (var group : command.getSubcommandGroups()) {
            final var groupData = new SubcommandGroupData(
                    group.getInfo().name(), group.getInfo().description());
            for (var entry : group.getSubcommands().entrySet()) {
                groupData.addSubcommands(subcommandData(entry.getKey(), entry.getValue(), autocompleted));
            }

            data.addSubcommandGroups(groupData);
        }
    }

    private SubcommandData subcommandData(String name, Subcommand subcommand, Set<String> autocompleted) {
        return new SubcommandData(name, subcommand.getInfo().description())
                .addOptions(optionData(subcommand.getOptions(), autocompleted));
    }

    List<OptionData> optionData(List<Option> options, Set<String> autocompleted) {
        final var out = new ArrayList<OptionData>(options.size());
        for (Option option : options) {
            final var info = option.getInfo();
            out.add(new OptionData(
                    info.type(),
                    info.name(),
                    info.description(),
                    info.required(),
                    autocompleted.contains(info.name())));
        }

        return out;
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        final var command = registry.find(event.getName());
        if (command.isEmpty()) {
            event.replyChoices(List.of()).queue();
            return;
        }

        final var focused = event.getFocusedOption();
        final var completer = command.get().prototype().getAutocompleters().get(focused.getName());
        if (completer == null) {
            event.replyChoices(List.of()).queue();
            return;
        }

        final Runnable task = () -> {
            List<Command.Choice> choices;
            try {
                choices = completer.complete(this, event, focused.getValue());
            } catch (Exception ex) {
                LOG.error("autocomplete for option `{}' of /{} failed", focused.getName(), event.getName(), ex);

                choices = List.of();
            }

            final var expected = focused.getType();
            final var matching = choices.stream()
                    .filter(choice -> choice.getType() == expected)
                    .toList();

            if (matching.size() != choices.size()) {
                LOG.warn(
                        "autocomplete for option `{}' of /{} returned {} choice(s) that aren't {}; dropping them",
                        focused.getName(),
                        event.getName(),
                        choices.size() - matching.size(),
                        expected);
            }

            event.replyChoices(matching).queue(null, ex -> {
                // Usually the 3-second window passing while the user kept typing; not worth an error.
                LOG.debug("autocomplete reply for /{} was rejected", event.getName(), ex);
            });
        };

        try {
            executor.execute(task);
        } catch (RejectedExecutionException ex) {
            event.replyChoices(List.of()).queue();
        }
    }

    @Override
    public boolean canUse(CommandInfo command, User user, Member member) {
        final var registered = registry.find(command.name());
        if (registered.isEmpty()) {
            return false;
        }

        final var chain = new ArrayList<>(interceptors);
        chain.addAll(registered.get().interceptors());

        for (var interceptor : chain) {
            try {
                if (!interceptor.isVisibleTo(user, member, command)) {
                    return false;
                }
            } catch (RuntimeException ex) {
                LOG.warn(
                        "interceptor {} failed in isVisibleTo; hiding /{}",
                        interceptor.getClass().getName(),
                        command.name(),
                        ex);

                return false;
            }
        }

        return true;
    }
}
