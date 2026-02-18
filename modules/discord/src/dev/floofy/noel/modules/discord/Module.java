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
import com.google.inject.Injector;
import dev.floofy.noel.modules.AbstractNoelModule;
import dev.floofy.noel.modules.annotations.Initializer;
import dev.floofy.noel.modules.annotations.Teardown;
import dev.floofy.noel.modules.settings.Setting;
import dev.floofy.noel.modules.settings.Settings;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.hooks.InterfacedEventManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@dev.floofy.noel.modules.annotations.Module(
        name = "discord",
        description = "Module that initializes JDA and makes it accessible to all modules",
        priority = 500
)
public final class Module extends AbstractNoelModule {
    private static final Logger LOG = LoggerFactory.getLogger(Module.class);

    @Override
    protected void configure() {
        bind(EventListener.class).asEagerSingleton();
        bind(JDA.class).toProvider(Provider.class);
    }

    @Initializer
    public void init(@NotNull JDA jda, @NotNull ExecutorService executorService) {
        executorService.submit(jda::awaitReady);
    }

    @Teardown
    public void teardown(@NotNull Injector injector) throws Exception {
        final JDA jda = injector.getInstance(JDA.class);
        jda.shutdown();

        if (!jda.awaitShutdown(10, TimeUnit.SECONDS)) {
            jda.shutdownNow();
            if (!jda.awaitShutdown(10, TimeUnit.SECONDS)) {
                LOG.warn("JDA couldn't be forcefully shutdown");
            }
        } else {
            LOG.warn("Noel has disconnected from Discord's wires! ^- ~ -^");
        }
    }

    static class Provider implements com.google.inject.Provider<JDA> {
        private final Setting<String> token = Setting.string("discord.token");
        private final EventListener eventListener;
        private final Settings settings;

        @Inject
        Provider(Settings settings, EventListener listener) {
            this.settings = settings;
            this.eventListener = listener;
        }

        @Override
        public JDA get() {
            return JDABuilder.createLight(settings.get(token, true), List.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS))
                    .setAutoReconnect(true)
                    .setStatus(OnlineStatus.IDLE)
                    .setEnableShutdownHook(false)
                    .setEventManager(new InterfacedEventManager())
                    .addEventListeners(eventListener)
                    .build();
        }
    }
}
