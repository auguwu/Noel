/*
 * 🐾✨ Noel: Discord bot made to manage my servers, made in Java.
 * Copyright 2021-2023 Noel <cutie@floofy.dev>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.floofy.noel.discord.commands.internal;

import dev.floofy.noel.discord.commands.AbstractCommand;
import dev.floofy.noel.discord.commands.annotations.Command;
import dev.floofy.noel.discord.commands.annotations.Option;
import dev.floofy.utils.kotlin.threading.ThreadFactoryKt;
import io.sentry.Sentry;
import jakarta.inject.Inject;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandHandler extends ListenerAdapter {
    private final ExecutorService executorService = Executors.newCachedThreadPool(ThreadFactoryKt.createThreadFactory(
            "Noel-CommandExecutor", null, Thread.currentThread().getThreadGroup()));

    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private final AtomicBoolean hasRegistered = new AtomicBoolean(false);
    private final Set<AbstractCommand> commands;

    @Inject
    public CommandHandler(Set<AbstractCommand> commands) {
        this.commands = commands;
    }

    /**
     * Returns all the registered commands in this handler.
     */
    @NotNull
    public Set<AbstractCommand> getCommands() {
        return commands;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        if (!hasRegistered.compareAndSet(false, true)) {
            return;
        }

        final JDA jda = event.getJDA();
        LOG.info("Registering {} slash commands...", commands.size());

        for (AbstractCommand command : commands) {
            final Command info = command.getClass().getDeclaredAnnotation(Command.class);
            if (info == null) {
                LOG.warn(
                        "Cannot register slash command class [{}] due to no @Command annotation",
                        command.getClass().getName());

                continue;
            }

            Field infoField;
            try {
                infoField = command.getClass().getSuperclass().getDeclaredField("info");
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }

            infoField.setAccessible(true);

            // should never happen
            try {
                infoField.set(command, info);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            // Get all the options from any field
            final Option[] options = Arrays.stream(command.getClass().getDeclaredFields())
                    .map(field -> field.getAnnotation(Option.class))
                    .filter(Objects::nonNull)
                    .toArray(Option[]::new);

            if (info.onlyInGuilds().length > 0) {
                LOG.info(
                        "Registering command /{} in only guilds [{}]",
                        info.name(),
                        String.join(
                                ", ",
                                Arrays.stream(info.onlyInGuilds())
                                        .mapToObj(Long::toString)
                                        .toList()));

                for (long guildId : info.onlyInGuilds()) {
                    final Guild guild = jda.getGuildById(guildId);
                    if (guild == null) {
                        LOG.warn(
                                "Not registering command /{} in guild [{}] since Noel is not in it",
                                info.name(),
                                guildId);

                        continue;
                    }

                    LOG.debug("Resolved guild id [{}] to guild {}", guildId, guild.getName());
                    final SlashCommandData data = Commands.slash(info.name(), info.description());
                    for (Option option : options) {
                        data.addOption(option.type(), option.name(), option.description(), option.required());
                    }

                    guild.upsertCommand(data)
                            .queue((cmd) -> LOG.info(
                                    "Upserted guild command {} ({}) successfully for guild {}",
                                    cmd.getName(),
                                    cmd.getId(),
                                    guild.getName()));
                }
            } else {
                LOG.info("Registering command /{} as a global command!", info.name());
                final SlashCommandData data = Commands.slash(info.name(), info.description());
                for (Option option : options) {
                    data.addOption(option.type(), option.name(), option.description(), option.required());
                }

                jda.upsertCommand(data)
                        .queue((cmd) ->
                                LOG.info("Upserted global command {} ({}) successfully", cmd.getName(), cmd.getId()));
            }
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        LOG.info(
                "Received slash command '/{}' by {}",
                event.getName(),
                event.getInteraction().getMember());

        final Optional<AbstractCommand> command = commands.stream()
                .filter(f -> f.getInfo().name().equals(event.getName()))
                .findAny();

        if (command.isEmpty()) {
            event.reply(":question: **| Command %s was not found**".formatted(event.getName()))
                    .setEphemeral(true)
                    .queue();

            return;
        }

        final Member currentMember = event.getMember();
        if (Sentry.isEnabled() && currentMember != null) {
            final User user = currentMember.getUser();
            final io.sentry.protocol.User sentryUser = new io.sentry.protocol.User();

            sentryUser.setName(user.getEffectiveName());
            sentryUser.setData(Map.of("id", user.getId()));

            Sentry.setUser(sentryUser);
        }

        final AbstractCommand cmd = command.get();
        event.reply("Hello, world!").queue();
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        LOG.info("Received modal action [{}] by {}", event.getModalId(), event.getMember());
    }
}
