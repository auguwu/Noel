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

package dev.floofy.noel.modules.discord;

import com.google.inject.Injector;
import dev.floofy.noel.config.SecureSetting;
import dev.floofy.noel.config.Setting;
import dev.floofy.noel.modules.AbstractNoelModule;
import dev.floofy.noel.modules.annotations.ModulePriority;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.hooks.InterfacedEventManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ModulePriority(500)
public class JDAModule extends AbstractNoelModule {
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private final SecureSetting discordToken =
            Setting.secureSetting("discord.token").resolve();

    @Override
    protected void configure() {
        final String token = discordToken.resolve();
        final JDABuilder builder = JDABuilder.create(
                        token, List.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS))
                .setIdle(true)
                .setAutoReconnect(true)
                .setStatus(OnlineStatus.IDLE)
                .setEnableShutdownHook(false)
                .setEventManager(new InterfacedEventManager())
                .addEventListeners(new GenericEventAdapter());

        bind(JDA.class).toInstance(builder.build());
        bind(JDAModule.class).toInstance(this);
    }

    @Override
    public void dispose(@NotNull Injector injector) throws Exception {
        final JDA jda = injector.getInstance(JDA.class);
        jda.shutdown();
        if (!jda.awaitShutdown(10, TimeUnit.SECONDS)) {
            jda.shutdownNow();
            if (!jda.awaitShutdown(10, TimeUnit.SECONDS)) {
                LOG.warn("JDA couldn't be forcefully shutdown.");
            }
        } else {
            LOG.warn("JDA instance was shut down successfully :>");
        }
    }

    @Override
    @NotNull
    public String name() {
        return "noel:jda";
    }
}
