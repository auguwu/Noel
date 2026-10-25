/// 🐾✨ Noel: Discord bot made to manage my servers made in Java
/// Copyright 2021-2026 Noel Towa <cutie@floofy.dev>
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.

package dev.floofy.noel.pinecone;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/// A [`@SubcommandGroup`][dev.floofy.noel.pinecone.annotations.SubcommandGroup]
/// class, its instance, and its subcommands.
///
/// Created by [AbstractSlashCommand#getSubcommandGroups()].
public final class SubcommandGroup {
    private final dev.floofy.noel.pinecone.annotations.SubcommandGroup info;
    private final Map<String, Subcommand> subcommands;
    private final Object instance;

    SubcommandGroup(
            dev.floofy.noel.pinecone.annotations.SubcommandGroup info,
            Object instance,
            Map<String, Subcommand> subcommands) {
        this.info = Objects.requireNonNull(info, "info");
        this.instance = Objects.requireNonNull(instance, "instance");
        this.subcommands =
                Collections.unmodifiableMap(new TreeMap<>(Objects.requireNonNull(subcommands, "subcommands")));
    }

    /// Returns the group's annotation.
    public dev.floofy.noel.pinecone.annotations.SubcommandGroup getInfo() {
        return info;
    }

    /// Returns the group's subcommands, by name, sorted.
    public Map<String, Subcommand> getSubcommands() {
        return subcommands;
    }

    /// Returns the instance of the group class that the subcommands run on.
    public Object getInstance() {
        return instance;
    }
}
