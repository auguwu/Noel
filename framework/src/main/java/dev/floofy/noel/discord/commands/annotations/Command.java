package dev.floofy.noel.discord.commands.annotations;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Command declaration for {@link dev.floofy.noel.discord.commands.AbstractCommand abstract commands} to attach
 * metadata.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
    /**
     * A short description about this command.
     */
    @NotNull
    String description() default "No description was specified.";

    /**
     * Whether if this command should be run by the owners or not.
     */
    boolean isOwnerOnly() default false;

    /**
     * List of guild IDs that this command should be only registered
     * in. By default, an empty long array will be a global command while
     * a non-empty long array will be registered in specific guilds.
     */
    long[] onlyInGuilds() default {};

    /**
     * List of channel IDs that this command should be only
     * executed in.
     */
    long[] onlyInChannels() default {};

    /**
     * Name of this command.
     */
    @NotNull
    String name();
}
