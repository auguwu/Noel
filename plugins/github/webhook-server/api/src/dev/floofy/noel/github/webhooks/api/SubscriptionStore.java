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

package dev.floofy.noel.github.webhooks.api;

import dev.floofy.noel.github.webhooks.models.Subscription;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/// Webhooks, stored in the `github_webhooks` MongoDB collection.
///
/// Every change takes the server's ID as well as the webhook's, and only
/// matches a webhook owned by that server. Webhook IDs show up in autocomplete
/// and in GitHub's settings, so a command run in one server must never be able
/// to change another server's webhook by passing its ID.
public interface SubscriptionStore {
    /// The most webhooks one server can have. Also Discord's limit on
    /// autocomplete suggestions, so every one can be picked from the list.
    int MAX_PER_GUILD = 25;

    /// Looks up a webhook by ID alone, for incoming deliveries.
    /// @param id the webhook's ID
    /// @return the webhook, or empty if there's none with that ID
    Optional<Subscription> find(String id);

    /// Lists a server's webhooks, oldest first.
    /// @param guild the server
    /// @return its webhooks
    List<Subscription> list(long guild);

    /// Saves a new webhook.
    /// @param subscription the webhook
    void save(Subscription subscription);

    /// Counts a server's webhooks.
    /// @param guild the server
    /// @return how many it has
    long count(long guild);

    /// Deletes one of a server's webhooks.
    /// @return `false` if the server has no webhook with that ID
    boolean delete(long guild, String id);

    /// Changes which events a webhook posts. An empty set means the defaults.
    /// @return `false` if the server has no webhook with that ID
    boolean setEvents(long guild, String id, Set<String> events);

    /// Moves a webhook to another channel.
    /// @return `false` if the server has no webhook with that ID
    boolean setChannel(long guild, String id, long channel);

    /// Replaces a webhook's secret.
    /// @return `false` if the server has no webhook with that ID
    boolean setSecret(long guild, String id, String secret);
}
