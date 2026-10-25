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

package dev.floofy.noel.discord;

import com.google.inject.Inject;
import com.google.inject.Provider;
import dev.floofy.noel.Setting;
import dev.floofy.noel.SettingException;
import dev.floofy.noel.Settings;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Logs the connection and rotates the bot's presence every [#ROTATE_EVERY].
///
/// Presences come from `plugins.discord.presences` (see [PresenceMeta]) and
/// are re-read on every rotation, so a configuration reload applies at the next
/// one. If the reloaded list is invalid, the previous list keeps rotating.
public final class EventListener extends ListenerAdapter implements AutoCloseable {
    private static final Setting<List<PresenceMeta>> PRESENCES =
            Setting.list("plugins.discord.presences", PresenceMeta::setting).orElse(List.of());
    private static final Duration ROTATE_EVERY = Duration.ofMinutes(5);
    private static final Activity FALLBACK = Activity.competing("for your love! <3");
    private static final Logger LOG = LoggerFactory.getLogger(EventListener.class);

    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicInteger next = new AtomicInteger();

    private final ScheduledExecutorService scheduler;
    private final Settings settings;
    private final Provider<JDA> jda;

    private volatile List<PresenceMeta> lastValidPresences;
    private volatile ScheduledFuture<?> task;

    @Inject
    EventListener(Settings settings, ScheduledExecutorService scheduler, Provider<JDA> jda) {
        this.scheduler = scheduler;
        this.settings = settings;
        this.jda = jda;
    }

    @Override
    public void onReady(@NonNull ReadyEvent event) {
        final var self = event.getJDA().getSelfUser();
        LOG.info("Bot has connected as {} ({})", self.getName(), self.getId());

        if (started.compareAndSet(false, true)) {
            task = scheduler.scheduleWithFixedDelay(
                    this::rotatePresence, 0, ROTATE_EVERY.toSeconds(), TimeUnit.SECONDS);
        }
    }

    @Override
    public void onShutdown(@NonNull ShutdownEvent event) {
        LOG.warn("received shutdown event! (closeCode={})", event.getCloseCode());
        this.close();
    }

    /// Stops rotating. Called when JDA shuts down, and by the Discord plugin before
    /// it does, so no rotation runs against a closing connection.
    @Override
    public void close() {
        if (this.task != null) {
            task.cancel(false);
        }
    }

    /// Shows the next presence, in configured order, wrapping around.
    ///
    /// Never throws: an exception escaping a `scheduleWithFixedDelay` task cancels
    /// every later run, which would silently freeze the presence.
    void rotatePresence() {
        try {
            final var presences = getLastValidPresences();
            if (presences.isEmpty()) {
                jda.get().getPresence().setPresence(OnlineStatus.ONLINE, FALLBACK);
                return;
            }

            final var meta = presences.get(Math.floorMod(next.getAndIncrement(), presences.size()));
            jda.get().getPresence().setPresence(OnlineStatus.ONLINE, meta.toActivity());
        } catch (Throwable ex) {
            LOG.error("failed to rotate presence; trying again in {}", ROTATE_EVERY, ex);
        }
    }

    private List<PresenceMeta> getLastValidPresences() {
        try {
            final var presences = this.settings.require(PRESENCES);
            this.lastValidPresences = presences;

            return presences;
        } catch (SettingException ex) {
            LOG.warn("ignoring invalid presence configuration, keeping the previous list:", ex);
            return this.lastValidPresences;
        }
    }
}
