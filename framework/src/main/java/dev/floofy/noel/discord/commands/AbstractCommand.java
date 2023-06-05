package dev.floofy.noel.discord.commands;

import dev.floofy.noel.discord.commands.annotations.Command;
import dev.floofy.noel.discord.commands.subcommands.AbstractSubcommand;
import dev.floofy.noel.discord.commands.subcommands.SubcommandGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AbstractCommand {
    public final List<SubcommandGroup> subcommandGroups = new ArrayList<>();
    public final List<AbstractSubcommand> subcommands = new ArrayList<>();
    protected final List<CommandOption<?>> options = new ArrayList<>();

    private Command info;

    public abstract void execute(@NotNull CommandContext context);

    public void registerSubcommandGroup(Class<? extends SubcommandGroup> clazz) {
    }

    public void registerSubcommand(Class<? extends AbstractSubcommand> clazz) {}

    @NotNull
    public Command getInfo() {
        if (info == null) {
            throw new IllegalStateException("Command class [" + getClass() + "] was not constructed correctly.");
        }

        return info;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> CommandOption<T> option(@NotNull String name) {
        Objects.requireNonNull(name, "Option name cannot be null.");

        return (CommandOption<T>) options
            .stream()
            .filter(opt -> opt.getInfo().value().equals(name))
            .findAny()
            .orElse(null);
    }
}
