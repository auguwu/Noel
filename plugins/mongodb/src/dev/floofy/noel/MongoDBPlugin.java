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
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import dev.floofy.noel.plugins.Descriptor;
import dev.floofy.noel.plugins.Plugin;
import java.util.concurrent.TimeUnit;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Connects to MongoDB and exports the [MongoClient] and the configured [MongoDatabase].
/// ```yaml
/// database:
///   uri: mongodb://localhost:27017
///   name: noel
/// ```
///
/// ## Lifecycle
/// - **Enable.** Creates the client and pings the server. If MongoDB can't be
///   reached within 10 seconds (or the URI's `serverSelectionTimeoutMS`), this
///   plugin fails to enable, by name, along with every plugin that depends on
///   it, instead of failing on the first query.
/// - **Destruct.** Closes the client. A later enable, through `/plugins enable`,
///   creates a new one and re-reads the configuration, so a changed URI applies.
///
/// ## Injecting the client
/// The exported bindings are unscoped: each injection returns the client from
/// the current enable. That makes re-enabling safe, as long as nothing keeps a
/// client from an earlier one. So:
///
/// - **Inject them after this plugin enables**, which is when your own plugin
///   enables, or later. A plugin's constructor runs while plugins are being
///   loaded, before anything is enabled, and injecting `MongoDatabase` there
///   fails with "MongoDB plugin isn't enabled".
///
/// - **Inject `Provider<MongoDatabase>` into anything long-lived**, like a
///   `@Singleton` store, and call `get()` when you need it. A store that holds a
///   `MongoDatabase` directly keeps using the old, closed client after this
///   plugin is disabled and re-enabled. Its own plugin is re-enabled too, but
///   Guice singletons in its injector survive that.
@Descriptor(
        id = "dev.floofy.noel.plugins.MongoDBPlugin",
        description = "Allows Noel to connect to a external MongoDB database for stateful data")
public final class MongoDBPlugin extends Plugin {
    private static final Setting<String> CONNECTION_URI = Setting.string("plugins.mongodb.uri");
    private static final Setting<String> DB_NAME = Setting.string("plugins.mongodb.db");
    private static final Logger LOG = LoggerFactory.getLogger(MongoDBPlugin.class);

    /// How long to wait for a server when the URI doesn't say. The driver's
    /// default is 30 seconds, which is a long time for startup to hang when
    /// MongoDB is down.
    private static final long DEFAULT_SERVER_SELECTION_TIMEOUT_SECONDS = 10;

    private volatile MongoClient client;
    private volatile MongoDatabase db;

    private final Settings settings;

    @Inject
    MongoDBPlugin(Settings settings) {
        this.settings = settings;
    }

    @Override
    protected void configure() {
        export(MongoDatabase.class);
        export(MongoClient.class);
    }

    @Provides
    MongoClient client() {
        return requireEnabled(this.client);
    }

    @Provides
    MongoDatabase db() {
        return requireEnabled(this.db);
    }

    @Override
    public void onPluginEnable() throws Exception {
        final var uri = new ConnectionString(settings.require(CONNECTION_URI));
        final var name = settings.require(DB_NAME);

        final var created = MongoClients.create(MongoClientSettings.builder()
                .applyToClusterSettings(cluster ->
                        cluster.serverSelectionTimeout(DEFAULT_SERVER_SELECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .applyConnectionString(uri)
                .applicationName("Noel")
                .build());

        final var db = created.getDatabase(name);
        try {
            final var started = System.nanoTime();
            db.runCommand(new Document("ping", 1));
            LOG.info(
                    "connected to MongoDB {} (db `{}') in {}ms",
                    uri.getHosts(),
                    name,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        } catch (MongoException ex) {
            created.close();
            throw new IllegalStateException(
                    "couldn't reach MongoDB at %s: %s".formatted(uri.getHosts(), ex.getMessage()), ex);
        }

        this.client = created;
        this.db = db;
    }

    @Override
    public void onPluginDestruct() {
        final var created = this.client;
        this.client = null;
        this.db = null;

        if (created != null) {
            created.close();
        }
    }

    private static <T> T requireEnabled(T value) {
        if (value == null) {
            throw new IllegalStateException(
                    "MongoDB plugin isn't enabled; inject Provider<MongoDatabase> and call get() later");
        }

        return value;
    }
}
