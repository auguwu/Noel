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

import java.util.ArrayList;
import java.util.List;

public class SubcommandGroup {
    private final List<AbstractSubcommand> subcommands = new ArrayList<>();
    private dev.floofy.noel.discord.commands.annotations.SubcommandGroup info;

    public String getName() {
        if (info == null) {
            throw new IllegalStateException("Subcommand group metadata was not properly implemented");
        }

        return info.name();
    }

    public String getDescription() {
        if (info == null) {
            throw new IllegalStateException("Subcommand group metadata was not properly implemented");
        }

        return info.description();
    }

    public List<AbstractSubcommand> getSubcommands() {
        return subcommands;
    }
}
