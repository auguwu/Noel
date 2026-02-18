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

import dev.floofy.noel.BuildInfo;
import dev.floofy.noel.pinecone.AbstractSlashCommand;
import dev.floofy.noel.pinecone.CommandContext;
import dev.floofy.noel.pinecone.annotations.SlashCommand;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@SlashCommand(name = "uptime", description = "Shows how long the bot has been running")
public class Uptime extends AbstractSlashCommand {
    @Override
    public void execute(CommandContext context) throws Exception {
        context.replyFormat(
                        ":timer: **%s**",
                        formatDuration(System.currentTimeMillis() - BuildInfo.getStartedAt()))
                .queue();
    }

    String formatDuration(long millis) {
        ArrayList<String> increments = new ArrayList<>();

        final long days = TimeUnit.DAYS.convert(millis, TimeUnit.MILLISECONDS);
        millis -= TimeUnit.DAYS.toMillis(days);

        final long hours = TimeUnit.HOURS.convert(millis, TimeUnit.MILLISECONDS);
        millis -= TimeUnit.HOURS.toMillis(hours);

        final long minutes = TimeUnit.MINUTES.convert(millis, TimeUnit.MILLISECONDS);
        millis -= TimeUnit.MINUTES.toMillis(minutes);

        final long seconds = TimeUnit.SECONDS.convert(millis, TimeUnit.MILLISECONDS);

        if (days > 0) {
            increments.add(article("day", days));
        }

        if (hours > 0) {
            increments.add(article("hour", hours));
        }

        if (minutes > 0) {
            increments.add(article("minute", minutes));
        }

        if (seconds > 0) {
            increments.add(article("second", seconds));
        }

        return String.join(", ", increments);
    }

    String article(String title, long time) {
        return time > 1
                ? String.format("%d %ss", time, title)
                : String.format("%d %s", time, title);
    }
}
