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

package dev.floofy.noel.modules.discord;

import com.google.inject.Inject;
import com.google.inject.Provider;
import dev.floofy.noel.modules.settings.Setting;
import dev.floofy.noel.modules.settings.Settings;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class EventListener extends ListenerAdapter {
    static class PresenceMeta {
        private final String message;
        private final Activity.ActivityType type;

        PresenceMeta(@NotNull String message, @NotNull Activity.ActivityType type) {
            this.message = message;
            this.type = type;
        }

        @NotNull
        public Activity.ActivityType getType() {
            return type;
        }

        @NotNull
        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return String.format("PresenceMeta(message=\"%s\", type=\"%s\")", getMessage(), getType().getKey());
        }
    }

    /// modules:
    ///   discord:
    ///     presenceMessages:
    ///       - message: "you"
    ///         status: watching
    private final Setting<List<PresenceMeta>> listOfPresences = Setting.of("modules.discord.presenceMessages", (value) -> {
        if (value == null) {
            return List.of();
        }

        if (!(value instanceof List<?>)) {
            throw new IllegalStateException("[modules.discord.presenceMessages] expected a list of objects of {message: string, status: string}");
        }

        final ArrayList<PresenceMeta> meta = new ArrayList<>();
        for (Object payload: ((List<?>) value)) {
            if (!(payload instanceof Map<?, ?> map)) {
                throw new IllegalArgumentException("list element is not a map");
            }

            final Object message = map.get("message");
            if (message == null) {
                throw new IllegalArgumentException("missing `message' parameter");
            }

            if (!(message instanceof String)) {
                throw new IllegalArgumentException("expected `message' parameter to be a string");
            }

            final Object type = map.get("type");
            if (type != null && !(type instanceof String)) {
                throw new IllegalArgumentException("expected `type' parameter to be a string");
            }

            Activity.ActivityType activityType = type == null
                    ? Activity.ActivityType.PLAYING
                    : switch (((String)type).toLowerCase(Locale.ROOT)) {
                        case "playing" -> Activity.ActivityType.PLAYING;
                        case "listening" -> Activity.ActivityType.LISTENING;
                        case "watching" -> Activity.ActivityType.WATCHING;
                        case "competing" -> Activity.ActivityType.COMPETING;
                        default -> throw new IllegalArgumentException(String.format("invalid kind \"%s\"", (String)type));
                    };

            meta.add(new PresenceMeta((String)message, activityType));
        }

        return meta;
    });

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AtomicBoolean taskShouldBeKilled = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler;
    private final Settings settings;
    private final Provider<JDA> jda;

    private ScheduledFuture<?> rotatePresenceTask = null;

    @Inject
    EventListener(Settings settings, ScheduledExecutorService scheduler, Provider<JDA> jda) {
        this.scheduler = scheduler;
        this.settings = settings;
        this.jda = jda;
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        log.warn("received shutdown event (closeCode={})", event.getCloseCode());
        if (rotatePresenceTask != null && !rotatePresenceTask.isCancelled()) {
            rotatePresenceTask.cancel(false);
        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        final SelfUser self = event.getJDA().getSelfUser();
        log.info("Bot has connected as {}#{} ({})",
                self.getName(),
                self.getDiscriminator(),
                self.getId());

        // Cancel existing task if reconnect
        if (rotatePresenceTask != null && !rotatePresenceTask.isCancelled()) {
            rotatePresenceTask.cancel(false);
        }

        rotatePresenceTask = scheduler.scheduleWithFixedDelay(
                this::rotatePresence,
                0,
                5,
                TimeUnit.MINUTES
        );
    }

    void rotatePresence() {
        final var presences = settings.get(listOfPresences);
        final JDA jda = this.jda.get();

        if (presences.isEmpty()) {
            jda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.competing("for your love! <3"));
            return;
        }

        int index = ThreadLocalRandom.current().nextInt(presences.size());
        final PresenceMeta meta = presences.get(index);

        log.info("rotated presence: {} ({})", meta.getMessage(), meta.getType());
        jda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.of(meta.getType(), meta.getMessage()));
    }
}
