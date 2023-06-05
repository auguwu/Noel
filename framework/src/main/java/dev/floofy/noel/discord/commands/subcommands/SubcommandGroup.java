package dev.floofy.noel.discord.commands.subcommands;

import com.google.inject.Guice;

import java.util.ArrayList;
import java.util.List;

public class SubcommandGroup {
    private final List<AbstractSubcommand> subcommands = new ArrayList<>();
    private dev.floofy.noel.discord.commands.annotations.SubcommandGroup info;

    public String getName() {
        if (info == null) {
            throw new IllegalStateException("Subcommand group metadata was not properly implemented");
        }

        return info.name();
    }

    public String getDescription() {
        if (info == null) {
            throw new IllegalStateException("Subcommand group metadata was not properly implemented");
        }

        return info.description();
    }

    public List<AbstractSubcommand> getSubcommands() {
        return subcommands;
    }
}
