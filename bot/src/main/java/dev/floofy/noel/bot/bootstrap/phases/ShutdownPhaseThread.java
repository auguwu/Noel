/*
 * 🐾✨ Noel: Discord bot made to manage my servers, made in Java.
 * Copyright 2021-2023 Noel <cutie@floofy.dev>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.floofy.noel.bot.bootstrap.phases;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import dev.floofy.noel.modules.NoelModule;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShutdownPhaseThread extends Thread {
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    public ShutdownPhaseThread() {
        setName("Noel-ShutdownThread");
    }

    @Override
    public void run() {
        final Injector injector = ConfigureModulesBootstrapPhase.getInjector();
        if (injector == null) {
            LOG.warn("Guice injector was not initialized, skipping module destruction");
            return;
        }

        LOG.info("Disposing of all modules...");
        final var bindings = injector.getBindings();
        for (Map.Entry<Key<?>, Binding<?>> entry : bindings.entrySet()) {
            final Binding<?> value = entry.getValue();
            final Object instance = value.getProvider().get();

            if (instance instanceof NoelModule module) {
                LOG.trace("Disposing module [{}]", module.getClass().getName());
                try {
                    module.dispose(injector);
                } catch (Exception e) {
                    LOG.error(
                            "Unable to dispose module [{}] correctly:",
                            module.getClass().getName(),
                            e);
                }
            }
        }

        LOG.info("Noel is no longer available to us. :(");
    }
}
