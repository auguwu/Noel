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
import dev.floofy.utils.kotlin.threading.ThreadFactoryKt;
import io.sentry.Sentry;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandHandler extends ListenerAdapter {
    private final ExecutorService executorService =
            Executors.newCachedThreadPool(ThreadFactoryKt.createThreadFactory("Noel-CommandExecutor", null, null));
    private final Logger LOG = LoggerFactory.getLogger(getClass());

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
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        LOG.info("Received modal action [{}] by {}", event.getModalId(), event.getMember());
    }
}
