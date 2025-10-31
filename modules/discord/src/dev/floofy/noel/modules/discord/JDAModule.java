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

import com.google.inject.Injector;
import dev.floofy.noel.modules.AbstractNoelModule;
import dev.floofy.noel.modules.annotations.Module;
import dev.floofy.noel.modules.annotations.Teardown;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.hooks.InterfacedEventManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Module(
        name = "discord",
        description = "Module that loads in the Discord slash commands and event handler",
        priority = 500
)
public final class JDAModule extends AbstractNoelModule {
    private static final Logger LOG = LoggerFactory.getLogger(JDAModule.class);

    @Override
    protected void configure() {
        final JDABuilder builder = JDABuilder.create("", List.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS))
                .setIdle(true)
                .setAutoReconnect(true)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .setEnableShutdownHook(false)
                .setEventManager(new InterfacedEventManager())
                .addEventListeners();

        bind(JDA.class).toInstance(builder.build());
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
}
