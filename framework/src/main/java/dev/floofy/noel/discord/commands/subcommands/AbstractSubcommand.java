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

package dev.floofy.noel.discord.commands.subcommands;

import dev.floofy.noel.discord.commands.CommandContext;
import dev.floofy.noel.discord.commands.CommandOption;
import dev.floofy.noel.discord.commands.annotations.Subcommand;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractSubcommand {
    private final List<CommandOption<?>> options = new ArrayList<>();
    private Subcommand info;

    public abstract void execute(CommandContext context);

    public String getName() {
        if (info == null) {
            throw new IllegalStateException("Subcommand metadata was not properly implemented");
        }

        return info.name();
    }

    public String getDescription() {
        if (info == null) {
            throw new IllegalStateException("Subcommand metadata was not properly implemented");
        }

        return info.description();
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
