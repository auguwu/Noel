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

import com.google.inject.Singleton;
import dev.floofy.noel.pinecone.api.CommandRegistry;
import dev.floofy.noel.pinecone.commands.Help;
import dev.floofy.noel.pinecone.impl.CommandRegistryImpl;
import dev.floofy.noel.pinecone.impl.PineconeImpl;
import dev.floofy.noel.plugins.Descriptor;
import dev.floofy.noel.plugins.Plugin;
import dev.floofy.noel.plugins.PluginContext;
import dev.floofy.noel.plugins.PluginLifecycleHook;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The slash command framework, as a plugin.
///
/// Any plugin that lists this one in [Descriptor#dependsOn()] gets its slash
/// commands discovered and registered automatically when it enables, and
/// unregistered when it is destructed:
///
/// ```java
/// @Descriptor(id = "dev.floofy.noel.plugins.Music", dependsOn = { PineconePlugin.class })
/// public final class Music extends Plugin {}
///
/// // anywhere under Music's package:
/// @SlashCommand(name = "play", description = "Play a song")
/// public final class Play extends AbstractSlashCommand {
///     private final VoiceManager voice; // the Music plugin's own binding
///
///     @Inject
///     Play(VoiceManager voice) {
///         this.voice = voice;
///     }
///
///     @Override
///     public void execute(CommandContext context) {  }
/// }
/// ```
///
/// ## Lifecycle
/// 1. **Enable.** Registers Pinecone's own commands (`/help`, `/ping`, ...), then
///    starts listening for interactions. Until every plugin has enabled,
///    interactions get a "still starting up" reply, since the registry is still
///    being filled in.
/// 2. **Each dependent enables.** Its commands are scanned, validated and
///    registered. An invalid command fails that dependent, by name, and leaves
///    every other plugin running.
/// 3. **Every plugin has enabled.** Commands are pushed to Discord, once JDA is
///    ready too, and again whenever the configuration is
///    [reloaded][Settings#reload()]. That makes changes to
///    `commands.<name>.onlyInGuilds` apply without a restart.
/// 4. **Each dependent is destructed.** Its commands are removed from the
///    registry, so they stop running immediately.
/// 5. **Destruct.** Stops listening, then waits up to 10 seconds for running
///    commands to finish.
///
/// Removing a plugin's commands from the registry doesn't remove them from
/// Discord: users still see them until the next sync, and get an "unknown
/// command" reply if they run one. That only matters for unloading plugins at
/// runtime, which the plugin manager doesn't do yet.
///
/// ## Exports
/// [Pinecone], for listing commands, adding [CommandInterceptor]s, and
/// triggering a resync. Dependents can inject it anywhere, including in
/// constructors.
@Descriptor(
        id = "dev.floofy.noel.PineconePlugin",
        description = "Slash command framework that powers everything!",
        dependsOn = DiscordPlugin.class)
public final class PineconePlugin extends Plugin implements PluginLifecycleHook {
    private static final Logger LOG = LoggerFactory.getLogger(PineconePlugin.class);

    private CommandRegistryImpl registry;
    private AutoCloseable reloadListener;
    private PineconeImpl pinecone;
    private JDA jda;

    @Override
    protected void configure() {
        bind(CommandRegistryImpl.class).in(Singleton.class);
        bind(PineconeImpl.class).in(Singleton.class);

        bind(CommandRegistry.class).to(CommandRegistryImpl.class);
        bind(Pinecone.class).to(PineconeImpl.class);

        export(Pinecone.class);
    }

    @Override
    public void onPluginEnable() throws Exception {
        this.jda = injector().getInstance(JDA.class);
        this.registry = injector().getInstance(CommandRegistryImpl.class);
        this.pinecone = injector().getInstance(PineconeImpl.class);

        var count = registry.register(descriptor().id(), injector(), Help.class);
        LOG.info("registered {} built-in slash command(s)", count);

        jda.addEventListener(pinecone);
        reloadListener = injector().getInstance(Settings.class).onReload(pinecone::resync);
    }

    @Override
    public void onDependentEnabled(PluginContext cx) {
        var id = cx.descriptor().id();
        var count = registry.register(id, cx.injector(), cx.type());

        LOG.info("registered {} slash command(s) from plugin [{}]", count, id);
    }

    @Override
    public void onDependentDestructing(PluginContext cx) {
        registry.unregisterAll(cx.descriptor().id());
    }

    @Override
    public void onPluginsReady() throws Exception {
        pinecone.markPluginsReady();
    }

    @Override
    public void onPluginDestruct() throws Exception {
        reloadListener.close();
        jda.removeEventListener(pinecone);
        pinecone.close();
        registry.unregisterAll(descriptor().id());
    }
}
