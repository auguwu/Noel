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
import com.google.inject.Injector;
import dev.floofy.noel.Pinecone;
import dev.floofy.noel.pinecone.AbstractSlashCommand;
import dev.floofy.noel.pinecone.Option;
import dev.floofy.noel.pinecone.annotations.SlashCommand;
import dev.floofy.noel.pinecone.java.Function4;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PineconeImpl extends ListenerAdapter implements Pinecone {
    private static final Logger LOG = LoggerFactory.getLogger(PineconeImpl.class);

    private final ExecutorService executor = Executors.newCachedThreadPool((runnable) -> new Thread(runnable, "Noel-CommandExecution"));
    private final AtomicBoolean registered = new AtomicBoolean();

    private Set<AbstractSlashCommand> commands;
    private Injector injector;

    @Inject
    PineconeImpl(@NotNull Set<AbstractSlashCommand> commands, @NotNull Injector injector) {
        this.injector = injector;
        this.commands = commands;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        if (!registered.compareAndSet(false, true)) return;

        final JDA jda = event.getJDA();
        LOG.info("Preparing to register {} slash commands", commands.size());

        CommandListUpdateAction globalCommands = jda.updateCommands();
        final HashMap<Long, CommandListUpdateAction> guildOnlySlashCommands = new HashMap<>();

        for (AbstractSlashCommand command: commands) {
            final var info = command.getInfo();
            final var data = Commands.slash(info.name(), info.description());

            attachSubcommands(command, data);
            addOptions(command.getOptions(), (type, name, description, required) -> {
                data.addOption(type, name, description, required);
                return null;
            });

            if (info.onlyInGuilds().length == 0) {
                LOG.info("Registering global slash command /{}", info.name());
                globalCommands = globalCommands.addCommands(data);
            } else {
                LOG.info("Registering guild-only slash command /{} in guilds: [{}]",
                        info.name(),
                        String.join(", ", Arrays.stream(info.onlyInGuilds())
                                .mapToObj(Long::toString)
                                .toList()));

                for (long id: info.onlyInGuilds()) {
                    final Guild guild = jda.getGuildById(id);
                    if (guild == null) {
                        LOG.warn("skipping registration as Noel is not in server");
                        continue;
                    }

                    final var commands = guildOnlySlashCommands.getOrDefault(guild.getIdLong(), guild.updateCommands());
                    guildOnlySlashCommands.put(guild.getIdLong(), commands.addCommands(data));
                }
            }
        }

        globalCommands.queue((commands) -> {
            LOG.info("Successfully upserted or updated {} global commands", commands.size());
        });

        for (var commands: guildOnlySlashCommands.values()) {
            commands.queue((cmds) -> {
                LOG.info("Successfully upserted or updated {} guild only commands", cmds.size());
            });
        }

        cleanupOldCommands(jda);
        registered.set(true);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        LOG.info("received slash command event :: /{} by {}", event.getName(), event.getInteraction().getMember());

        final var command = getSlashCommands()
                .stream()
                .filter(cmd -> cmd.getInfo().name().equals(event.getName()))
                .findFirst();

        if (command.isEmpty()) {
            event.reply(String.format(":question: **Unknown slash command: /%s", event.getName())).setEphemeral(true).queue();
            return;
        }

        executor.execute(() -> Dispatcher.dispatch(command.get(), new CommandContextImpl(event.getInteraction(), this)));
    }

    @Override
    public Set<AbstractSlashCommand> getSlashCommands() {
        return commands;
    }

    void attachSubcommands(AbstractSlashCommand command, SlashCommandData data) {
        for (var entry: command.getSubcommands().entrySet()) {
            final SubcommandData subData = new SubcommandData(entry.getKey(), entry.getValue().getInfo().description());
            addOptions(entry.getValue().getOptions(), (type, name, description, required) -> {
                subData.addOption(type, name, description, required);
                return null;
            });

            data.addSubcommands(subData);
        }
    }

    void addOptions(List<Option> options, Function4<OptionType, String, String, Boolean, Void> addOption) {
        for (Option option: options) {
            final dev.floofy.noel.pinecone.annotations.Option info = option.getInfo();
            addOption.invoke(
                info.type(),
                info.name(),
                info.description(),
                info.required()
            );
        }
    }

    void cleanupOldCommands(@NotNull JDA jda) {
        LOG.info("Cleaning up old global and global commands...");

        final var currentSlashCommands = getSlashCommands()
                .stream()
                .map(cmd -> cmd.getInfo().name())
                .toList();

        jda.retrieveCommands().queue(commands -> {
            for (Command cmd: commands) {
                if (!currentSlashCommands.contains(cmd.getName())) {
                    LOG.warn("Found obsolete global slash command: /{}", cmd.getName());
                    cmd.delete().queue(
                        success -> LOG.info("Deleted obsolete global slash command"),
                        ex -> LOG.error("failed to delete slash command", ex)
                    );
                }
            }
        });

        for (Guild guild: jda.getGuilds()) {
            final var currentSlashCommandsForGuild = getSlashCommands()
                    .stream()
                    .map(AbstractSlashCommand::getInfo)
                    .filter(info -> Arrays.stream(info.onlyInGuilds()).anyMatch(id -> guild.getIdLong() == id))
                    .map(SlashCommand::name)
                    .toList();

            guild.retrieveCommands().queue(commands -> {
                for (Command cmd: commands) {
                    if (!currentSlashCommandsForGuild.contains(cmd.getName())) {
                        LOG.warn("[{} ({})] Found obsolete guild-only slash command: /{}", guild.getName(), guild.getId(), cmd.getName());
                        cmd.delete().queue(
                                success -> LOG.info("[{} ({})] Deleted obsolete guild-only slash command", guild.getName(), guild.getId()),
                                ex -> LOG.error("[{} ({})] failed to delete slash command", guild.getName(), guild.getId(), ex)
                        );
                    }
                }
            });
        }
    }
}
