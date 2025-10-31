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

package dev.floofy;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.floofy.noel.modules.AbstractNoelModule;
import dev.floofy.noel.modules.ModuleLocator;
import net.dv8tion.jda.api.JDA;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOError;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class Noel {
    private static final Map<Integer, Function<Object, Boolean>> CODES = new HashMap<>();
    private static final Logger LOG = LoggerFactory.getLogger(Noel.class);

    static {
        CODES.putAll(Map.of(
            128, (i) -> i instanceof InternalError,
            127, (i) -> i instanceof OutOfMemoryError,
            126, (i) -> i instanceof StackOverflowError,
            125, (i) -> i instanceof UnknownError,
            124, (i) -> i instanceof IOError,
            123, (i) -> i instanceof LinkageError
        ));
    }


    private static Noel instance;

    private final List<AbstractNoelModule> modules = ModuleLocator.loadModules();
    private Injector injector;

    public static void onShutdown() {
        if (instance == null) return;

        LOG.warn("Shutting down...");
        if (instance.injector == null) {
            LOG.warn("will not shutdown as no Guice injector is available (required for module teardown)");
            return;
        }

        for (var mod: getInstance().getModules()) {
            LOG.warn("tearing down module {}[{}]", mod.getInfo().name(), mod.getClass().getName());

            final var teardownMethod = mod.getTeardownMethod();
            if (teardownMethod != null && teardownMethod.canAccess(mod)) {
                try {
                    teardownMethod.invoke(mod, getInstance().getInjector());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    LOG.warn("failed to teardown module {}[{}]:", mod.getInfo().name(), mod.getClass().getName(), e);
                }
            }
        }

        instance = null;
        LOG.warn("goodbye!! ^TwT^");
    }

    private void installDefaultThreadExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            if (ex instanceof Error) {
                LOG.error("uncaught fatal exception in thread {} ({}):", thread.getName(), thread.threadId(), ex);

                for (var entry: CODES.entrySet()) {
                    if (entry.getValue().apply(ex)) {
                        Runtime.getRuntime().halt(entry.getKey());
                    }
                }

                System.exit(1);
            } else {
                LOG.error("uncaught user-land exception in thread {} ({}):", thread.getName(), thread.threadId(), ex);
            }
        });
    }

    public void bootstrap() throws InterruptedException {
        if (instance == null) {
            instance = this;
        }

        // Initialize the thread exception thread
        installDefaultThreadExceptionHandler();

        assert injector == null : "cannot call `bootstrap()` twice";
        injector = Guice.createInjector(modules);

        // Initialize our modules
        for (var mod: getModules()) {
            final var initMethod = mod.getInitMethod();
            if (initMethod != null && initMethod.canAccess(mod)) {
                try {
                    initMethod.invoke(mod, injector);
                } catch(IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(String.format("Failed to initialize module %s", mod.getInfo().name()), e);
                }
            }
        }

        // Get the initialized `JDA` instance. It should be the second thing
        // to be injected already.
        final JDA jda = injector.getInstance(JDA.class);
        jda.awaitReady();
    }

    public static Noel getInstance() {
        assert instance != null : "you must bootstrap before getting an instance";
        return instance;
    }

    public List<AbstractNoelModule> getModules() {
        return Collections.unmodifiableList(modules);
    }

    @NotNull
    public Injector getInjector() {
        assert injector != null : "you must bootstrap before getting a Guice injector";
        return injector;
    }
}
