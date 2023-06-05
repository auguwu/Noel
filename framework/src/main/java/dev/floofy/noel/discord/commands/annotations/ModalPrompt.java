package dev.floofy.noel.discord.commands.annotations;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a modal prompt when one was requested.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModalPrompt {
    /**
     * The ID of the modal prompt that this prompt should
     * execute in.
     */
    @NotNull
    String value();
}
