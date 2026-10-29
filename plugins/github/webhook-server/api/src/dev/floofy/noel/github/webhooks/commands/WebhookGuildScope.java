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

package dev.floofy.noel.github.webhooks.commands;

import com.google.inject.Inject;
import dev.floofy.noel.Setting;
import dev.floofy.noel.Settings;
import dev.floofy.noel.pinecone.api.GuildScope;

import java.util.List;
import java.util.Set;

public final class WebhookGuildScope implements GuildScope {
    private static final Setting<List<Long>> ONLY_IN = Setting.list("plugins.github.webhooks.onlyIn", Setting::snowflake);

    @Inject
    private Settings settings;

    @Override
    public Set<Long> guilds() {
        return settings.get(ONLY_IN).map(Set::copyOf).orElse(Set.of());
    }
}
