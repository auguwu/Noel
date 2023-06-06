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

import dev.floofy.noel.discord.commands.annotations.Command;
import dev.floofy.noel.discord.commands.subcommands.AbstractSubcommand;
import dev.floofy.noel.discord.commands.subcommands.SubcommandGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractCommand {
    public final List<SubcommandGroup> subcommandGroups = new ArrayList<>();
    public final List<AbstractSubcommand> subcommands = new ArrayList<>();
    protected final List<CommandOption<?>> options = new ArrayList<>();

    private Command info;

    public abstract void execute(@NotNull CommandContext context);

    public void registerSubcommandGroup(Class<? extends SubcommandGroup> clazz) {}

    public void registerSubcommand(Class<? extends AbstractSubcommand> clazz) {}

    @NotNull
    public Command getInfo() {
        if (info == null) {
            throw new IllegalStateException("Command class [" + getClass() + "] was not constructed correctly.");
        }

        return info;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> CommandOption<T> option(@NotNull String name) {
        Objects.requireNonNull(name, "Option name cannot be null.");

        return (CommandOption<T>) options.stream()
                .filter(opt -> opt.getInfo().name().equals(name))
                .findAny()
                .orElse(null);
    }
}
