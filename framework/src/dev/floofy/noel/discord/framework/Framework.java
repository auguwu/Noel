/// 🐾✨ Noel: Discord bot made to manage my servers made in Java
/// Copyright 2021-2025 Noel Towa <cutie@floofy.dev>
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

package dev.floofy.noel.discord.framework;

import com.google.inject.Inject;
import dev.floofy.noel.ThreadFactory;
import dev.floofy.noel.discord.framework.annotations.Option;
import dev.floofy.noel.discord.framework.impl.NoelCommandContext;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Framework extends ListenerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(Framework.class);

    private final ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory("Noel-CommandExecutor"));
    private final AtomicBoolean registered = new AtomicBoolean();
    private final Set<AbstractCommand> commands;

    @Inject
    public Framework(Set<AbstractCommand> commands) {
        this.commands = commands;
    }

    public Set<AbstractCommand> getCommands() {
        return commands;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        if (!registered.compareAndSet(false, true)) return;

        final var jda = event.getJDA();
        LOG.info("Preparing to register {} slash commands!", commands.size());

        final var global = jda.updateCommands();
        final Map<Long, CommandListUpdateAction> guildCommands = new HashMap<>();

        for (var command: getCommands()) {
            final var info = command.getInfo();
            final var data = Commands.slash(info.name(), info.description());
            for (var opt: command.getOptions()) {
                data.addOption(
                        opt.getInfo().type(),
                        opt.getInfo().name(),
                        opt.getInfo().description(),
                        opt.getInfo().required());
            }

            if (info.onlyInGuilds().length == 0) {
                LOG.info("Registering global slash command /{}", info.name());
                global.addCommands(data);
            } else {
                LOG.info("Registering guild-only slash command /{} in guilds: {}",
                        info.name(),
                        String.join(
                                "; ",
                                Arrays.stream(info.onlyInGuilds())
                                        .mapToObj(Long::toString)
                                        .toList())
                );

                for (long id: info.onlyInGuilds()) {
                    final var guild = jda.getGuildById(id);
                    if (guild == null) {
                        LOG.warn("skipping registration due to Noel not being in said server");
                        continue;
                    }

                    if (!guildCommands.containsKey(guild.getIdLong())) {
                        guildCommands.put(guild.getIdLong(), guild.updateCommands());
                    }

                    final var commands = guildCommands.get(guild.getIdLong());
                    commands.addCommands(data);
                    guildCommands.put(guild.getIdLong(), commands);
                }
            }
        }

        // Upsert or update global commands
        global.queue(commands -> {
            LOG.info("Upserted or updated {} global commands", commands.size());
        });

        // For each guild, upsert or update commands
        for (var cmds: guildCommands.entrySet()) {
            cmds.getValue().queue(commands -> {
                LOG.info("Upserted or updated {} commands in guild {}", commands.size(), cmds.getKey());
            });
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        LOG.info("Received slash command event :: /{} by {}", event.getName(), event.getInteraction().getMember());

        final var command = getCommands()
                .stream()
                .filter(cmd -> cmd.getInfo().name().equals(event.getName()))
                .findAny();

        if (command.isEmpty()) {
            event.reply(String.format(":question: --=-- Unknown slash command: **/%s**", event.getName()))
                    .setEphemeral(true)
                    .queue();

            return;
        }

        final var cmd = command.get();
        final CommandContext context = new NoelCommandContext(event);

        executor.execute(() -> {
            try {
                cmd.execute(context);
            } catch (Exception e) {
                LOG.error("slash command /{} failed:", event.getName(), e);
                event.reply(":pensive: **^-~-^** Command has failed! Try again later, maybe?")
                        .setEphemeral(true)
                        .queue();
            }
        });
    }
}
