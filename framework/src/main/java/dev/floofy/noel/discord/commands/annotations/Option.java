package dev.floofy.noel.discord.commands.annotations;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as an option that can be registered and read from. The field
 * has to be a {@link dev.floofy.noel.discord.commands.CommandOption} or this
 * will not work correctly.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Option {
    /**
     * Option type to use when registering this command. This can't be
     * a subcommand or subcommand group as they have their own implementation
     * within a {@link dev.floofy.noel.discord.commands.AbstractCommand}.
     */
    @NotNull
    OptionType type() default OptionType.STRING;

    /**
     * Whether if this option is required to be executed
     * or not.
     */
    boolean required() default false;

    /**
     * Option name.
     */
    @NotNull
    String value();
}
