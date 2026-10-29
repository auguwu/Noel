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

package dev.floofy.noel.github.webhooks.models;

import dev.floofy.noel.github.webhooks.api.Event;
import java.time.Instant;
import java.util.*;
import org.bson.Document;

/// One GitHub webhook, and where its events are posted.
///
/// @param id         the webhook's ID, in its payload URL (`?id=...`)
/// @param guildId    the server that owns it
/// @param channelId  the channel events are posted in
/// @param repository only post events from this repository (`owner/name`,
///                   lowercase), or `null` for any. Useful for an organization
///                   webhook that sends every repository's events.
/// @param events     the events to post; empty means the [defaults][WebhookEvent#defaults()]
/// @param secret     the secret GitHub signs deliveries with
/// @param createdBy  the user who created it
/// @param createdAt  when it was created
public record Subscription(
        String id,
        long guildId,
        long channelId,
        String repository,
        Set<String> events,
        String secret,
        long createdBy,
        Instant createdAt) {
    public Subscription {
        events = Collections.unmodifiableSet(new LinkedHashSet<>(events));
        repository = repository == null ? null : repository.toLowerCase(Locale.ROOT);
    }

    /// The events this webhook posts, with the defaults filled in.
    /// @return the event names
    public Set<String> effectiveEvents() {
        return events.isEmpty() ? new LinkedHashSet<>(Event.defaults()) : events;
    }

    /// Whether a delivery should be posted.
    /// @param event          the `X-GitHub-Event` header
    /// @param repositoryName the payload's `repository.full_name`, or `null`
    /// @return whether both the event and the repository match
    public boolean accepts(String event, String repositoryName) {
        if (repository != null && !repository.equalsIgnoreCase(repositoryName)) {
            return false;
        }

        final var normalized = event.equals("watch") ? "star" : event;
        return effectiveEvents().contains(normalized);
    }

    public Document toDocument() {
        return new Document("_id", id)
                .append("guild", guildId)
                .append("channel", channelId)
                .append("repository", repository)
                .append("events", List.copyOf(events))
                .append("secret", secret)
                .append("created_by", createdBy)
                .append("created_at", Date.from(createdAt));
    }

    public static Subscription fromDocument(Document doc) {
        return new Subscription(
                doc.getString("_id"),
                doc.getLong("guild"),
                doc.getLong("channel"),
                doc.getString("repository"),
                new LinkedHashSet<>(doc.getList("events", String.class, List.of())),
                doc.getString("secret"),
                doc.getLong("created_by"),
                doc.getDate("created_at").toInstant());
    }

    /// Hides the secret, so a subscription can be logged.
    @Override
    public String toString() {
        return "Subscription(id=%s, guildId=%d, channelId=%d, repository=%s, events=%s)"
                .formatted(id, guildId, channelId, repository, events);
    }
}
