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

import java.lang.reflect.Method;
import java.util.List;

/// Information about a slash command's subcommand.
public final class Subcommand {
    private final dev.floofy.noel.pinecone.annotations.Subcommand info;
    private final List<Option> options;
    private final Method methodHandle;
    private final Object instance;

    Subcommand(
            @NotNull dev.floofy.noel.pinecone.annotations.Subcommand info,
            @NotNull List<Option> options,
            @NotNull Method handle,
            @NotNull Object instance
    ) {
        this.methodHandle = handle;
        this.instance = instance;
        this.options = options;
        this.info = info;
    }

    public dev.floofy.noel.pinecone.annotations.Subcommand getInfo() {
        return info;
    }

    public void invoke(@NotNull CommandContext context) throws Exception {
        final var interaction = context.getInteraction();
    }
}