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

package dev.floofy.noel.birthdays.impl;

import com.google.inject.Inject;
import com.google.inject.Provider;
import dev.floofy.noel.Settings;
import dev.floofy.noel.birthdays.Config;
import dev.floofy.noel.birthdays.api.Calendar;
import dev.floofy.noel.birthdays.api.Scheduler;
import dev.floofy.noel.birthdays.models.Birthday;
import java.time.Clock;
import java.util.EnumSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SchedulerImpl implements Scheduler {
    private static final Logger LOG = LoggerFactory.getLogger(SchedulerImpl.class);

    /// How long [#close()] waits for a running tick before giving up on it.
    private static final long CLOSE_TIMEOUT_SECONDS = 10;

    private final ScheduledExecutorService executor;
    private final Provider<JDA> jda;
    private final Settings settings;
    private final StoreImpl store;
    private final Clock clock;

    /// Held for the whole of each tick, so [#close()] can wait for one to finish.
    private final ReentrantLock ticking = new ReentrantLock();

    /// Checked inside [#ticking], so a tick that was already dispatched when [#close()] ran does nothing.
    private volatile boolean running;
    private ScheduledFuture<?> task;

    @Inject
    SchedulerImpl(
            StoreImpl store, Settings settings, Provider<JDA> jda, ScheduledExecutorService executor, Clock clock) {
        this.store = store;
        this.settings = settings;
        this.jda = jda;
        this.executor = executor;
        this.clock = clock;
    }

    @Override
    public synchronized void start() throws Exception {
        if (this.task != null) {
            throw new IllegalStateException("birthday scheduler is already running");
        }

        this.running = true;
        this.task = this.executor.scheduleWithFixedDelay(this::tick, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public void close() throws Exception {
        if (this.task == null) {
            return;
        }

        this.running = false;
        this.task.cancel(false);
        this.task = null;

        try {
            if (ticking.tryLock(CLOSE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                ticking.unlock();
            } else {
                LOG.warn("a birthday tick is still running after {}s; not waiting for it", CLOSE_TIMEOUT_SECONDS);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    void tick() {
        this.ticking.lock();
        try {
            if (!this.running) {
                return;
            }

            final var now = this.clock.instant();
            for (var birthday : this.store.due(now)) {
                if (!this.running) {
                    return;
                }

                process(birthday);
            }
        } catch (Throwable ex) {
            LOG.error("birthday scheduler tick failed; retrying next minute", ex);
        } finally {
            this.ticking.unlock();
        }
    }

    void process(Birthday birthday) {
        final var zone = birthday.zone();
        final var end = Calendar.endOfDay(birthday.nextAt(), zone);
        final var year = Calendar.yearOf(birthday.nextAt(), zone);
        final var next = Calendar.next(birthday.day(), zone, end, year);

        // Claimed by an overlapping tick, or edited by the member just now.
        if (!store.advance(birthday, next, year)) {
            return;
        }

        if (!clock.instant().isBefore(end)) {
            LOG.info(
                    "missed birthday of {} in guild {} (the day ended at {}); next is {}",
                    birthday.user(),
                    birthday.guild(),
                    end,
                    next);

            return;
        }

        announce(birthday);
    }

    private void announce(Birthday birthday) {
        final var config = Config.of(this.settings, birthday.guild());
        if (config.isEmpty()) {
            LOG.debug("no birthday config configured for guild {}", birthday.guild());
            return;
        }

        final var channelId = config.map(Config::channelId).get();
        final var channel = jda.get().getTextChannelById(channelId);
        if (channel == null) {
            LOG.warn("birthday channel {} for guild {} doesn't exist or isn't visible", channelId, birthday.guild());

            return;
        }

        final var roleId = config.map(Config::roleId).get();
        final var message =
                config.map(Config::message).orElse(":birthday: **|> Happy birthday, <@%user.id%>!** <@%role.id%>");
        channel.sendMessage(message.replace("%user.id%", Long.toString(birthday.user()))
                        .replace("%role.id%", Long.toString(roleId)))
                .setAllowedMentions(EnumSet.of(Message.MentionType.USER, Message.MentionType.ROLE))
                .mentionUsers(birthday.user())
                .mentionRoles(roleId)
                .queue(
                        ok -> LOG.info("announced birthday of {} in guild {}", birthday.user(), birthday.guild()),
                        ex -> LOG.error("failed to announce birthday of {}:", birthday.user(), ex));
    }
}
