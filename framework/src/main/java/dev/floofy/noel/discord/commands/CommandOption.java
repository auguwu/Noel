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

package dev.floofy.noel.discord.commands;

import dev.floofy.noel.discord.commands.annotations.Option;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an option that can be used in an {@link AbstractCommand}.
 * @param <T> Resolved type for this {@link CommandOption}.
 */
public class CommandOption<T> {
    private final OptionMapping mapping;
    private final Class<T> tClass;
    private final Option info;

    protected CommandOption(@Nullable OptionMapping mapping, @NotNull Class<T> cls, @NotNull Option info) {
        this.mapping = mapping;
        this.tClass = cls;
        this.info = info;
    }

    /**
     * Returns the {@link Option} metadata that was present in
     * the field.
     */
    @NotNull
    public Option getInfo() {
        return info;
    }

    /**
     * Resolves the given {@link CommandOption}, can return null if it was not present.
     */
    @Nullable
    public T resolve() {
        // This can be null if JDA couldn't find the option that
        // we were looking for.
        if (mapping == null) return null;

        // this is going to get ugly.
        Object found;
        if (tClass == String.class) {
            found = mapping.getAsString();
        } else if (tClass == long.class) {
            found = mapping.getAsLong();
        } else if (tClass == double.class) {
            found = mapping.getAsDouble();
        } else if (tClass == int.class) {
            found = mapping.getAsInt();
        } else if (tClass == boolean.class) {
            found = mapping.getAsBoolean();
        } else if (tClass == Member.class) {
            found = mapping.getAsMember();
        } else if (tClass == User.class) {
            found = mapping.getAsUser();
        } else if (tClass == IMentionable.class) {
            found = mapping.getAsMentionable();
        } else if (tClass == Message.Attachment.class) {
            found = mapping.getAsAttachment();
        } else if (tClass == Role.class) {
            found = mapping.getAsRole();
        } else {
            throw new IllegalStateException("Unable to find mapping for class [" + tClass + "]");
        }

        return tClass.cast(found);
    }
}
