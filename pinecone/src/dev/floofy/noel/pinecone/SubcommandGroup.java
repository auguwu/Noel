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

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class SubcommandGroup {
    private final dev.floofy.noel.pinecone.annotations.SubcommandGroup info;
    private final Map<String, Subcommand> subcommands;
    private final Object instance;

    SubcommandGroup(
            @NotNull dev.floofy.noel.pinecone.annotations.SubcommandGroup info,
            @NotNull Object instance,
            @NotNull Map<String, Subcommand> subcommands) {
        this.info = info;
        this.instance = instance;
        this.subcommands = subcommands;
    }

    public dev.floofy.noel.pinecone.annotations.SubcommandGroup getInfo() {
        return info;
    }

    public Map<String, Subcommand> getSubcommands() {
        return subcommands;
    }

    public Object getInstance() {
        return instance;
    }
}
