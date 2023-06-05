/*
 * 🐾✨ Noel: Discord bot made to manage my servers, made in Java.
 * Copyright 2021-2023 Noel <cutie@floofy.dev>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.floofy.noel.discord.commands.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

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
