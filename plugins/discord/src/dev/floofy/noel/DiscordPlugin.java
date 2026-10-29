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

package dev.floofy.noel;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import dev.floofy.noel.discord.EventListener;
import dev.floofy.noel.plugins.Descriptor;
import dev.floofy.noel.plugins.Plugin;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.hooks.InterfacedEventManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Connects to Discord and exports the [JDA] instance.
///
/// JDA is a singleton built when this plugin enables, not when something first
/// injects it, so no gateway connection is opened while plugins are still
/// loading. Plugins that use JDA list this plugin in [Descriptor#dependsOn()].
@Descriptor(
        id = "dev.floofy.noel.DiscordPlugin",
        description = "Connects to Discord and makes JDA accessible to other plugins")
public final class DiscordPlugin extends Plugin {
    private static final Setting<List<GatewayIntent>> INTENTS =
            Setting.list("discord.intents", path -> Setting.enumValue(path, GatewayIntent.class));
    private static final Setting<String> TOKEN = Setting.string("discord.token");
    private static final Logger LOG = LoggerFactory.getLogger(DiscordPlugin.class);

    private final Settings settings;
    private JDA jda;

    @Inject
    DiscordPlugin(Settings settings) {
        this.settings = settings;
    }

    @Override
    protected void configure() {
        bind(EventListener.class).in(Singleton.class);
        export(JDA.class);
    }

    @Provides
    @Singleton
    JDA jda(EventListener listener) {
        final var intents = settings.get(INTENTS).orElse(List.of());
        final var token = settings.require(TOKEN);

        return JDABuilder.createLight(token, intents)
                .setAutoReconnect(true)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .setEnableShutdownHook(false)
                .setEventManager(new InterfacedEventManager())
                .addEventListeners(listener)
                .build();
    }

    @Override
    public void onPluginEnable() {
        jda = injector().getInstance(JDA.class);
    }

    @Override
    public void onPluginDestruct() throws InterruptedException {
        injector().getInstance(EventListener.class).close();
        jda.shutdown();
        if (jda.awaitShutdown(10, TimeUnit.SECONDS)) {
            LOG.warn("Noel has disconnected from Discord's wires! ^- ~ -^");
            return;
        }

        jda.shutdownNow();
        if (!jda.awaitShutdown(10, TimeUnit.SECONDS)) {
            LOG.warn("JDA couldn't be forcefully shutdown");
        }
    }
}
