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
import org.jetbrains.annotations.NotNull;

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
