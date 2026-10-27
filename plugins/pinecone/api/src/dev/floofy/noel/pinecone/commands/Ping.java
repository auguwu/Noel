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

import com.google.inject.Inject;
import dev.floofy.noel.Setting;
import dev.floofy.noel.Settings;
import dev.floofy.noel.pinecone.annotations.SlashCommand;
import dev.floofy.noel.pinecone.api.AbstractSlashCommand;
import dev.floofy.noel.pinecone.api.CommandContext;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@SlashCommand(name = "ping", description = "pong!")
public final class Ping extends AbstractSlashCommand {
    private static final Setting<List<String>> RESPONSES = Setting.list("commands.ping.responses", Setting::string);
    private final List<String> listOfResponses;

    @Inject
    Ping(Settings settings) {
        this.listOfResponses = settings.require(RESPONSES);
    }

    @Override
    public void execute(CommandContext cx) throws Exception {
        final long start = System.nanoTime();
        var message = listOfResponses.get(ThreadLocalRandom.current().nextInt(listOfResponses.size()));

        cx.reply(message).queue(hook -> {
            final var end = TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            hook.editOriginalFormat(":ping_pong: **|> %s** (*%d*ms)", message, end)
                    .queue();
        });
    }
}
