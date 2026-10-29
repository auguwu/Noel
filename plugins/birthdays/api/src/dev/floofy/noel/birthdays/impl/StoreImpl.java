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

package dev.floofy.noel.birthdays.impl;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Updates;
import dev.floofy.noel.birthdays.api.Store;
import dev.floofy.noel.birthdays.models.Birthday;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.bson.Document;

public final class StoreImpl implements Store {
    /// How many due birthdays a single scheduler tick handles.
    private static final int BATCH_SIZE = 100;
    private final Provider<MongoDatabase> db;

    @Inject
    StoreImpl(Provider<MongoDatabase> db) {
        this.db = db;
    }

    MongoCollection<Document> collection() {
        return this.db.get().getCollection("birthdays");
    }

    public void createIndexes() {
        collection().createIndex(Indexes.ascending("next_at"));
    }

    @Override
    public Optional<Birthday> find(long guild, long user) {
        final var result =
                collection().find(Filters.eq("_id", Birthday.id(guild, user))).first();

        return Optional.ofNullable(result).map(Birthday::fromDocument);
    }

    @Override
    public void save(Birthday birthday) {
        collection()
                .replaceOne(
                        Filters.eq("_id", Birthday.id(birthday.guild(), birthday.user())),
                        birthday.toDocument(),
                        new ReplaceOptions().upsert(true));
    }

    @Override
    public boolean delete(long guild, long user) {
        return collection()
                        .deleteOne(Filters.eq("_id", Birthday.id(guild, user)))
                        .getDeletedCount()
                > 0;
    }

    /// Returns birthdays whose day has started by `now`, earliest first.
    /// @param now the current instant
    /// @return at most one batch of due birthdays
    List<Birthday> due(Instant now) {
        return collection()
                .find(Filters.lte("next_at", Date.from(now)))
                .limit(BATCH_SIZE)
                .map(Birthday::fromDocument)
                .into(new ArrayList<>());
    }

    /// Moves a birthday on to its next occurrence, but only if nobody has changed
    /// it since it was read. This is what makes each birthday fire at most once,
    /// even if two ticks overlap or the member edits it mid-tick.
    ///
    /// @param birthday      the birthday as it was read
    /// @param next          its next occurrence
    /// @param celebratedYear the year being celebrated now
    /// @return `true` if this call claimed the occurrence
    boolean advance(Birthday birthday, Instant next, int celebratedYear) {
        final var result = collection()
                .updateOne(
                        Filters.and(
                                Filters.eq("_id", Birthday.id(birthday.guild(), birthday.user())),
                                Filters.eq("nextAt", Date.from(birthday.nextAt()))),
                        Updates.combine(
                                Updates.set("next_at", Date.from(next)),
                                Updates.set("last_celebrated_year", celebratedYear)));

        return result.getModifiedCount() == 1;
    }
}
