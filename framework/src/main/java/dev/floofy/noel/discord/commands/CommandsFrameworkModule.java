package dev.floofy.noel.discord.commands;

import dev.floofy.noel.discord.commands.internal.CommandHandler;
import dev.floofy.noel.modules.AbstractNoelModule;
import org.jetbrains.annotations.NotNull;

public class CommandsFrameworkModule extends AbstractNoelModule {
    @Override
    @NotNull
    public String name() {
        return "noel:commands";
    }

    @Override
    protected void configure() {
        bind(CommandHandler.class).asEagerSingleton();
    }
}
