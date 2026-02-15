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

package dev.floofy.noel.modules.mongodb;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import dev.floofy.noel.modules.AbstractNoelModule;
import dev.floofy.noel.modules.settings.Setting;
import dev.floofy.noel.modules.settings.Settings;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@dev.floofy.noel.modules.annotations.Module(
        name = "mongodb",
        description = "Provides a module for initializing a MongoDB database",
        priority = 900
)
public final class Module extends AbstractNoelModule {
    @Override
    protected void configure() {
        bind(MongoClient.class).toProvider(ClientProvider.class);
        bind(MongoDatabase.class).toProvider(DatabaseProvider.class);
    }

    static class ClientProvider implements Provider<MongoClient> {
        private static final Setting<String> CONNECTION_URI = Setting.string("database.uri", true);
        private final Settings settings;

        @Inject
        ClientProvider(@NotNull Settings settings) {
            this.settings = settings;
        }

        @Override
        public MongoClient get() {
            return MongoClients.create(MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(Objects.requireNonNull(settings.get(CONNECTION_URI))))
                    .applicationName("Noel")
                    .build());
        }
    }

    static class DatabaseProvider implements Provider<MongoDatabase> {
        private static final Setting<String> DATABASE_NAME = Setting.string("database.name", true);
        private final MongoClient client;
        private final Settings settings;

        @Inject
        DatabaseProvider(@NotNull Settings settings, @NotNull MongoClient client) {
            this.settings = settings;
            this.client = client;
        }

        @Override
        public MongoDatabase get() {
            return client.getDatabase(Objects.requireNonNull(settings.get(DATABASE_NAME)));
        }
    }
}
