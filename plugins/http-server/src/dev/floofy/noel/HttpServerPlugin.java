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
import dev.floofy.noel.http.Server;
import dev.floofy.noel.http.server.impl.NettyServerImpl;
import dev.floofy.noel.plugins.Descriptor;
import dev.floofy.noel.plugins.Plugin;

/// Runs Noel's HTTP server, and lets other plugins add routes to it.
///
/// The server listens on `plugins.http.server.address` (default `127.0.0.1`) and
/// `plugins.http.server.port` (default `3621`):
///
/// ```yaml
/// http:
///   server:
///     address: 127.0.0.1
///     port: 8080
/// ```
///
/// ## Adding routes
/// Depend on this plugin and inject [Server]. Add routes when your plugin
/// enables, and remove them when it is destructed, so that disabling your
/// plugin with `/plugins disable` also takes its routes down:
///
/// ```java
/// @Descriptor(id = "dev.floofy.noel.plugins.Webhooks", dependsOn = {HttpServerPlugin.class})
/// public final class WebhooksPlugin extends Plugin {
///     private final Server server;
///     private Router.Registration route;
///
///     @Inject
///     WebhooksPlugin(Server server) {
///         this.server = server;
///     }
///
///     @Override
///     public void onPluginEnable() {
///         route = server.getRouter().post("/webhooks/github", (req, res) -> res.sendText("ok"));
///     }
///
///     @Override
///     public void onPluginDestruct() {
///         route.close();
///     }
/// }
/// ```
///
/// ## Lifecycle
/// - **Enable.** Binds the port, then returns. Nothing waits on the server: Netty
///   accepts connections on its own threads. If the port can't be bound (it's
///   in use, say), this plugin fails to enable, along with every plugin that
///   depends on it, and the error is logged.
/// - **Destruct.** Stops accepting connections, closes open ones, and stops
///   Netty's threads. Every dependent has already been destructed, so their
///   routes are gone by then.
///
/// The address and port are read on every enable, so after changing them,
/// `/plugins disable` and `/plugins enable` this plugin to apply them without
/// restarting the bot.
///
/// ## Exports
/// [Server], whose [Server#getRouter()] is where routes go.
@Descriptor(
        id = "dev.floofy.noel.plugins.HttpServerPlugin",
        description = "A HTTP server that other plugins can add routes to")
public final class HttpServerPlugin extends Plugin {
    private Server server;

    @Override
    protected void configure() {
        bind(NettyServerImpl.class).in(Singleton.class);
        bind(Server.class).to(NettyServerImpl.class);

        export(Server.class);
    }

    @Override
    public void onPluginEnable() throws Exception {
        this.server = injector().getInstance(Server.class);
        server.start();
    }

    @Override
    public void onPluginDestruct() throws Exception {
        this.server.shutdown();
    }
}
