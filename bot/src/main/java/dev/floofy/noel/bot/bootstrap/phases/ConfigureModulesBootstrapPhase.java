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

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.floofy.noel.bot.bootstrap.BootstrapPhase;
import dev.floofy.noel.modules.ModuleLocator;
import dev.floofy.noel.modules.NoelModule;
import dev.floofy.noel.modules.jda.NoelJDAModule;
import dev.floofy.utils.java.SetOnce;
import java.util.List;
import net.dv8tion.jda.api.JDA;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class ConfigureModulesBootstrapPhase implements BootstrapPhase {
    private static final SetOnce<Injector> injectorSetOnce = new SetOnce<>();
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    /**
     * Returns the {@link Injector} that was created at bootstrap-time, this is only used
     * by the {@link ShutdownPhaseThread} to destroy modules.
     */
    @Nullable
    public static Injector getInjector() {
        return injectorSetOnce.getValueOrNull();
    }

    @Override
    public void bootstrap() throws Exception {
        final List<NoelModule> modules = ModuleLocator.loadModules();
        final Injector injector = Guice.createInjector(modules);
        injectorSetOnce.setValue(injector);

        final JDA jda = injector.getInstance(JDA.class);
        LOG.info("Now configuring JDA-based modules!");
        for (NoelModule mod :
                modules.stream().filter(i -> i instanceof NoelJDAModule).toList()) {
            ((NoelJDAModule) mod).configure(jda);
        }

        LOG.info("Guice has been initialized successfully, now running bot!");
        MDC.remove("bootstrap.phase");
        jda.awaitReady();
    }

    @Override
    @NotNull
    public String mdcValue() {
        return "configure:modules";
    }
}
