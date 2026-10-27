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

package dev.floofy.noel.pinecone.impl;

import dev.floofy.noel.Pinecone;
import dev.floofy.noel.pinecone.api.AutocompleteContext;
import java.util.Objects;
import net.dv8tion.jda.api.interactions.commands.CommandAutoCompleteInteraction;

public final class AutocompleteContextImpl implements AutocompleteContext {
    private final CommandAutoCompleteInteraction interaction;
    private final Pinecone pinecone;

    public AutocompleteContextImpl(CommandAutoCompleteInteraction interaction, Pinecone pinecone) {
        this.interaction = Objects.requireNonNull(interaction, "interaction");
        this.pinecone = Objects.requireNonNull(pinecone, "pinecone");
    }

    @Override
    public CommandAutoCompleteInteraction getInteraction() {
        return interaction;
    }

    @Override
    public Pinecone getPinecone() {
        return pinecone;
    }
}
