package dev.floofy.noel.discord.commands;

import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a context that belongs to an {@link AbstractCommand}.
 */
public interface CommandContext {
    /**
     * The {@link SlashCommandInteraction} that this context is holding
     * onto when being used by the {@link dev.floofy.noel.discord.commands.internal.CommandHandler}.
     */
    @NotNull
    SlashCommandInteraction getInteraction();
}
