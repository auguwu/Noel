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

package dev.floofy.noel.github.webhooks.impl;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import dev.floofy.noel.github.webhooks.api.SubscriptionStore;
import dev.floofy.noel.github.webhooks.models.Subscription;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.bson.Document;
import org.bson.conversions.Bson;

public final class SubscriptionStoreImpl implements SubscriptionStore {
    private final Provider<MongoDatabase> db;

    @Inject
    SubscriptionStoreImpl(Provider<MongoDatabase> db) {
        this.db = db;
    }

    private MongoCollection<Document> collection() {
        return db.get().getCollection("github_webhooks");
    }

    public void createIndexes() {
        collection().createIndex(Indexes.ascending("guildId"));
    }

    @Override
    public Optional<Subscription> find(String id) {
        return Optional.ofNullable(collection().find(Filters.eq("_id", id)).first())
                .map(Subscription::fromDocument);
    }

    @Override
    public List<Subscription> list(long guild) {
        return collection()
                .find(Filters.eq("guild", guild))
                .sort(Sorts.ascending("created_at"))
                .limit(MAX_PER_GUILD)
                .map(Subscription::fromDocument)
                .into(new ArrayList<>());
    }

    @Override
    public void save(Subscription subscription) {
        collection()
                .replaceOne(
                        Filters.eq("_id", subscription.id()),
                        subscription.toDocument(),
                        new ReplaceOptions().upsert(true));
    }

    @Override
    public long count(long guild) {
        return collection().countDocuments(Filters.eq("guild", guild));
    }

    @Override
    public boolean delete(long guild, String id) {
        return collection().deleteOne(owned(guild, id)).getDeletedCount() == 1;
    }

    @Override
    public boolean setEvents(long guild, String id, Set<String> events) {
        return update(guild, id, Updates.set("events", List.copyOf(events)));
    }

    @Override
    public boolean setChannel(long guild, String id, long channel) {
        return update(guild, id, Updates.set("channel", channel));
    }

    @Override
    public boolean setSecret(long guild, String id, String secret) {
        return update(guild, id, Updates.set("secret", secret));
    }

    private boolean update(long guild, String id, Bson update) {
        return collection().updateOne(owned(guild, id), update).getMatchedCount() == 1;
    }

    private static Bson owned(long guild, String id) {
        return Filters.and(Filters.eq("_id", id), Filters.eq("guild", guild));
    }
}
