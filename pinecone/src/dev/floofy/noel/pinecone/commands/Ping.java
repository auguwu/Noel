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

package dev.floofy.noel.pinecone.commands;

import dev.floofy.noel.pinecone.AbstractSlashCommand;
import dev.floofy.noel.pinecone.CommandContext;
import dev.floofy.noel.pinecone.annotations.SlashCommand;

import java.util.concurrent.TimeUnit;

@SlashCommand(name = "ping", description = "Pong!")
public class Ping extends AbstractSlashCommand {
    // private final Setting<List<String>> listOfResponses = new Setting<>("commands.ping.listOfResponses", List.of());

    @Override
    public void execute(CommandContext context) throws Exception {
        final long start = System.nanoTime();
        context.getInteraction().reply("ur gay lmao").queue(hook -> {
            final var end = TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            hook.editOriginal(String.format(":ping_pong: **%dms** ^w^", end)).queue();
        });
    }
}
