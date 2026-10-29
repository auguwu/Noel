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

package dev.floofy.noel.github.webhooks.internals;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.floofy.noel.github.api.Client;
import dev.floofy.noel.github.api.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Display names of GitHub users, which webhook payloads don't include.
@Singleton
public final class Usernames {
    private static final Duration TTL = Duration.ofHours(6);
    private static final Logger LOG = LoggerFactory.getLogger(Usernames.class);

    private record GitHubUser(String login, String name) {}

    private record Cached(Optional<String> name, Instant at) {}

    private final ConcurrentMap<String, Cached> cache = new ConcurrentHashMap<>();
    private final Client github;
    private final Clock clock;

    @Inject
    Usernames(Client github, Clock clock) {
        this.github = github;
        this.clock = clock;
    }

    /// Returns a user's display name, or empty if they haven't set one or the lookup fails.
    public Optional<String> of(String login) {
        if (login == null) {
            return Optional.empty();
        }

        LOG.debug("finding cached name for login [@{}]", login);

        final var key = login.toLowerCase(Locale.ROOT);
        final var cached = cache.get(key);
        if (cached != null && cached.at().plus(TTL).isAfter(clock.instant())) {
            LOG.debug("cache hit: {}", cached.name().isPresent() ? "<none>" : cached.name());
            return cached.name();
        }

        Optional<String> name;
        try {
            final var user = github.request(Method.GET, "/users/:username", Map.of("username", login))
                    .as(GitHubUser.class)
                    .complete();

            name = Optional.ofNullable(user.name()).filter(value -> !value.isBlank());
        } catch (RuntimeException ex) {
            LOG.debug("failed to lookup the name for login [{}]", login, ex);
            return Optional.empty();
        }

        LOG.debug("cache miss - adding: {}", name.isPresent() ? "<none>" : name);
        cache.put(key, new Cached(name, clock.instant()));
        return name;
    }
}
