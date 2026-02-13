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

import dev.floofy.noel.Pinecone;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;

public interface CommandContext {
    /// Returns the [slash command interaction][SlashCommandInteraction] that is associated
    /// when a command was executed.
    SlashCommandInteraction getInteraction();

    /// Returns an instance of a [pinecone][Pinecone].
    Pinecone getPinecone();
}
