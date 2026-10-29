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

package dev.floofy.noel.github.webhooks;

import com.google.inject.Singleton;
import dev.floofy.noel.DiscordPlugin;
import dev.floofy.noel.HttpServerPlugin;
import dev.floofy.noel.MongoDBPlugin;
import dev.floofy.noel.PineconePlugin;
import dev.floofy.noel.github.GitHubPlugin;
import dev.floofy.noel.github.webhooks.api.EventFormatter;
import dev.floofy.noel.github.webhooks.api.Receiver;
import dev.floofy.noel.github.webhooks.api.SubscriptionStore;
import dev.floofy.noel.github.webhooks.impl.ReceiverImpl;
import dev.floofy.noel.github.webhooks.impl.SubscriptionStoreImpl;
import dev.floofy.noel.github.webhooks.internals.Usernames;
import dev.floofy.noel.http.Server;
import dev.floofy.noel.http.server.Router;
import dev.floofy.noel.plugins.Descriptor;
import dev.floofy.noel.plugins.Plugin;

/// Posts GitHub webhook events to Discord channels.
///
/// Server admins manage webhooks with `/webhooks`, which hands them a payload
/// URL and secret to paste into GitHub. GitHub then sends events to
/// `POST /webhooks/github?id=<webhook ID>` on the HTTP server, and Noel posts
/// them to the webhook's channel. See [Receiver] for how a delivery is
/// checked and filtered.
///
/// ```yaml
/// plugins:
///     github:
///         webhooks:
///             # A list of guilds where the webhook server can react in.
///             enabledIn: [743698927039283201]
///
///             # Where GitHub can reach the HTTP server: a public HTTPS address, usually
///             # a reverse proxy in front of http.server.port.
///             public_url: https://noel.example.com
/// ```
///
/// ## Lifecycle
/// - **Enable.** Creates the MongoDB index, then adds the route. Deliveries
///   that arrive while the plugin is disabled get `404` from the HTTP server,
///   and GitHub lets you redeliver them from the webhook's settings.
///
/// - **Destruct.** Removes the route.
@Descriptor(
        id = "dev.floofy.noel.plugins.github.WebhooksPlugin",
        description = "Posts GitHub webhook events to Discord channels",
        dependsOn = {
            DiscordPlugin.class,
            PineconePlugin.class,
            HttpServerPlugin.class,
            MongoDBPlugin.class,
            GitHubPlugin.class
        })
public final class GitHubWebhooksPlugin extends Plugin {
    private Router.Registration route;

    @Override
    protected void configure() {
        bind(EventFormatter.class);
        bind(Usernames.class);

        bind(SubscriptionStoreImpl.class).in(Singleton.class);
        bind(SubscriptionStore.class).to(SubscriptionStoreImpl.class);

        bind(ReceiverImpl.class).in(Singleton.class);
        bind(Receiver.class).to(ReceiverImpl.class);
    }

    @Override
    public void onPluginEnable() throws Exception {
        injector().getInstance(SubscriptionStoreImpl.class).createIndexes();

        final var server = injector().getInstance(Server.class);
        final var receiver = injector().getInstance(Receiver.class);

        this.route = server.getRouter().post("/webhooks/github", receiver::handle);
    }

    @Override
    public void onPluginDestruct() throws Exception {
        if (route != null) {
            route.close();
            route = null;
        }
    }
}
