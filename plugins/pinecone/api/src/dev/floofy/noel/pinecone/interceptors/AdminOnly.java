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

package dev.floofy.noel.pinecone.interceptors;

import com.google.inject.Inject;
import dev.floofy.noel.Setting;
import dev.floofy.noel.Settings;
import dev.floofy.noel.pinecone.CommandContext;
import dev.floofy.noel.pinecone.CommandInfo;
import dev.floofy.noel.pinecone.CommandInterceptor;
import java.util.List;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

public final class AdminOnly implements CommandInterceptor {
    private static final Setting<List<Long>> OWNERS = Setting.list("discord.admins", AdminOnly::userId);
    private final List<Long> owners;

    @Inject
    AdminOnly(Settings settings) {
        this.owners = settings.get(OWNERS).orElse(List.of());
    }

    @Override
    public boolean before(CommandContext cx, CommandInfo command) throws Exception {
        final var user = cx.getUser();
        if (this.owners.contains(user.getIdLong())) {
            return true;
        }

        cx.reply(":no_entry: **|> only the admins can use this command!**")
                .setEphemeral(true)
                .queue();
        return false;
    }

    @Override
    public boolean isVisibleTo(User user, Member member, CommandInfo command) {
        return this.owners.contains(user.getIdLong());
    }

    private static Setting<Long> userId(String path) {
        return Setting.of(path, Object.class).transform(raw -> switch (raw) {
            case Long id -> id;
            case Integer id -> id.longValue();
            case String id -> Long.parseUnsignedLong(id.strip());
            default -> throw new IllegalArgumentException(String.format("expected a user ID, got: %s", raw));
        });
    }
}
