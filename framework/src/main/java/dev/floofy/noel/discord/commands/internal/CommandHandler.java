package dev.floofy.noel.discord.commands.internal;

import dev.floofy.noel.discord.commands.AbstractCommand;
import dev.floofy.utils.kotlin.threading.ThreadFactoryKt;
import io.sentry.Sentry;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommandHandler extends ListenerAdapter {
    private final ExecutorService executorService = Executors.newCachedThreadPool(ThreadFactoryKt.createThreadFactory("Noel-CommandExecutor", null, null));
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
        LOG.info("Received slash command '/{}' by {}",
            event.getName(),
            event.getInteraction().getMember());

        final Optional<AbstractCommand> command = commands
            .stream()
            .filter(f -> f.getInfo().name().equals(event.getName()))
            .findAny();

        if (command.isEmpty()) {
            event.reply(":question: **| Command %s was not found**".formatted(event.getName())).setEphemeral(true).queue();
            return;
        }

        final Member currentMember = event.getMember();
        if (Sentry.isEnabled() && currentMember != null) {
            final User user = currentMember.getUser();
            final io.sentry.protocol.User sentryUser = new io.sentry.protocol.User();

            sentryUser.setName(user.getEffectiveName());
            sentryUser.setData(Map.of(
                "id", user.getId()
            ));

            Sentry.setUser(sentryUser);
        }

        final AbstractCommand cmd = command.get();
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        LOG.info("Received modal action [{}] by {}", event.getModalId(), event.getMember());
    }
}
