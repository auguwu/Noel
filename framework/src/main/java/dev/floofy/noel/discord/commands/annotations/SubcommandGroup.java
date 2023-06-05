package dev.floofy.noel.discord.commands.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a group of {@link Subcommand subcommands} linked together.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SubcommandGroup {
    String description() default "No description was provided";
    String name();
}
