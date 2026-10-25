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

package dev.floofy;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.floofy.noel.Settings;
import dev.floofy.noel.plugins.PluginManager;

import java.awt.*;
import java.io.IOError;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;

public final class Noel {
    public static final String[] PLUGIN_PACKAGES = {"dev.floofy.noel"};
    public static final Color EMBED_COLOUR = Color.decode("#f4b5d5");
    private static final Logger LOG = LoggerFactory.getLogger(Noel.class);

    private static final Map<Integer, Predicate<Throwable>> CODES = new LinkedHashMap<>();

    static {
        CODES.put(128, InternalError.class::isInstance);
        CODES.put(127, OutOfMemoryError.class::isInstance);
        CODES.put(126, StackOverflowError.class::isInstance);
        CODES.put(125, UnknownError.class::isInstance);
        CODES.put(124, IOError.class::isInstance);
        CODES.put(123, LinkageError.class::isInstance);
    }

    private final AtomicBoolean shuttingDown = new AtomicBoolean();
    private final CountDownLatch stopped = new CountDownLatch(1);
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2, runnable -> new Thread(runnable, "Noel-ScheduledExecutorService"));

    private final ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger count = new AtomicInteger();

        @Override
        public Thread newThread(@NonNull Runnable runnable) {
            final var thread = new Thread(runnable, "Noel-ExecutorService[%d]".formatted(count.incrementAndGet()));
            thread.setDaemon(true);
            return thread;
        }
    });

    private volatile PluginManager plugins;
    private volatile Injector injector;

    public Noel() {}

    /// Loads the configuration, builds the core injector, loads and enables every
    /// plugin, then blocks until [#shutdown()] is called.
    ///
    /// @throws IllegalStateException if called more than once
    /// @throws Exception if the configuration cannot be loaded, or plugins fail to
    ///         load. Individual plugins failing to *enable* is logged, not thrown.
    public void bootstrap() throws Exception {
        if (this.injector != null) {
            throw new IllegalStateException("`bootstrap()` cannot be called more than once");
        }

        installDefaultThreadExceptionHandler();

        final Settings settings = Settings.load();
        installReloadSignalHandler(settings);

        this.injector = Guice.createInjector(binder -> {
            binder.bind(ScheduledExecutorService.class).toInstance(scheduler);
            binder.bind(ExecutorService.class).toInstance(executor);
            binder.bind(Settings.class).toInstance(settings);
            binder.bind(Noel.class).toInstance(this);

            binder.bind(PluginManager.class);
        });

        this.plugins = injector.getInstance(PluginManager.class);
        final var loaded = this.plugins.loadAll(Noel.class.getClassLoader(), PLUGIN_PACKAGES);
        final var enabled = this.plugins.enableAll();

        if (enabled.size() == loaded.size()) {
            LOG.info("all {} plugin(s) are enabled", enabled.size());
        } else {
            LOG.warn("{} of {} plugin(s) are enabled! see the errors above ^^^", enabled.size(), loaded.size());
        }

        stopped.await();
    }

    /// Destructs every plugin, then shuts down the shared executors. Only the first
    /// call does anything.
    public void shutdown() {
        if (!shuttingDown.compareAndSet(false, true)) {
            return;
        }

        LOG.warn("Noel is shutting down!");
        try {
            if (this.plugins != null) {
                plugins.close();
            }
        } catch (Exception ex) {
            LOG.error("failed to destroy plugins:", ex);
        } finally {
            shutdownExecutor(this.scheduler);
            shutdownExecutor(this.executor);

            stopped.countDown();
            LOG.warn("Goodbye! ^TwT^");

            // Flush and close the appenders only after our last log line.
            if (LoggerFactory.getILoggerFactory() instanceof ch.qos.logback.classic.LoggerContext context) {
                context.stop();
            }
        }
    }

    private void installReloadSignalHandler(Settings settings) {
        try {
            Signal.handle(new Signal("HUP"), _ -> {
                if (shuttingDown.get()) {
                    return;
                }

                LOG.info("received `SIGUP` signal, reloading configuration");
                try {
                    settings.reload();
                } catch (Exception ex) {
                    LOG.error("failed to reload configuration; keeping the current one", ex);
                }
            });
        } catch (IllegalArgumentException _) {
            LOG.warn("SIGHUP is not available on this platform; reloading by signal is disabled");
        }
    }

    private static void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException _) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void installDefaultThreadExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            if (!(ex instanceof Error)) {
                LOG.error("uncaught userland exception in thread {} ({}):", thread.getName(), thread.threadId(), ex);

                return;
            }

            LOG.error("uncaught fatal exception in thread {} ({}):", thread.getName(), thread.threadId(), ex);

            for (var entry : CODES.entrySet()) {
                if (entry.getValue().test(ex)) {
                    Runtime.getRuntime().halt(entry.getKey());
                }
            }

            System.exit(1);
        });
    }
}
