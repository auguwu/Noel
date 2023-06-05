package dev.floofy.noel.discord.commands.subcommands;

import dev.floofy.noel.discord.commands.CommandContext;
import dev.floofy.noel.discord.commands.CommandOption;
import dev.floofy.noel.discord.commands.annotations.Subcommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AbstractSubcommand {
    private final List<CommandOption<?>> options = new ArrayList<>();
    private Subcommand info;

    public abstract void execute(CommandContext context);

    public String getName() {
        if (info == null) {
            throw new IllegalStateException("Subcommand metadata was not properly implemented");
        }

        return info.name();
    }

    public String getDescription() {
        if (info == null) {
            throw new IllegalStateException("Subcommand metadata was not properly implemented");
        }

        return info.description();
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
