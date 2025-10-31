/// 🐾✨ Noel: Discord bot made to manage my servers made in Java
/// Copyright 2021-2025 Noel Towa <cutie@floofy.dev>
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

package dev.floofy.noel.modules.discord;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventListener extends ListenerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(EventListener.class);

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        final SelfUser user = event.getJDA().getSelfUser();
        LOG.info("Successfully connected to the Discord wires as {}#{} ({})", user.getName(), user.getDiscriminator(), user.getId());

        event.getJDA().getPresence().setPresence(
            OnlineStatus.ONLINE,
            Activity.competing("for your love!~ <3"),
            false
        );
    }
}
