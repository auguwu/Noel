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

import dev.floofy.noel.modules.settings.Setting;
import dev.floofy.noel.modules.settings.Settings;
import dev.floofy.noel.pinecone.AbstractSlashCommand;
import dev.floofy.noel.pinecone.CommandContext;
import dev.floofy.noel.pinecone.annotations.SlashCommand;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@SlashCommand(name = "ping", description = "Pong!")
public class Ping extends AbstractSlashCommand {
    private static final Setting<List<String>> LIST_OF_RESPONSES =
            Setting.of(
                    "commands.ping.listOfResponses",
                    (value) -> {
                        if (value == null) {
                            return List.of("^w^");
                        }

                        if (!(value instanceof List<?>)) {
                            throw new IllegalStateException(
                                    "[commands.ping.listOfResponses] expected a list of strings");
                        }

                        for (Object payload : ((List<?>) value)) {
                            if (!(payload instanceof String)) {
                                throw new IllegalArgumentException("list element is not a map");
                            }
                        }

                        //noinspection unchecked
                        return Collections.unmodifiableList((List<String>) value);
                    });

    private final List<String> listOfResponses;

    @Inject
    Ping(@NotNull Settings settings) {
        this.listOfResponses = settings.get(LIST_OF_RESPONSES);
    }

    @Override
    public void execute(CommandContext context) throws Exception {
        final long start = System.nanoTime();
        int index = ThreadLocalRandom.current().nextInt(listOfResponses.size());
        final String message = listOfResponses.get(index);

        context.reply(message)
                .queue(
                        hook -> {
                            final long end =
                                    TimeUnit.MILLISECONDS.convert(
                                            System.nanoTime() - start, TimeUnit.NANOSECONDS);
                            hook.editOriginal(
                                            String.format(
                                                    ":ping_pong: **%dms** (%s)", end, message))
                                    .queue();
                        });
    }
}
