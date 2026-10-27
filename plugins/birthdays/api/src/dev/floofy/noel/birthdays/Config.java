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

package dev.floofy.noel.birthdays;

import dev.floofy.noel.Setting;
import dev.floofy.noel.Settings;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Which servers have birthdays turned on, and where each announces them.
///
/// A server opts in by having an entry under `birthdays`, keyed by its ID. The
/// value is either the announcement channel's ID, or a map with a `channel`:
///
/// ```yaml
/// birthdays:
///   "824066105102303232": "1001338219591585792"   # short form
///   "998877665544332211":
///     channel: "1122334455667788990"
/// ```
///
/// A server without an entry has birthdays turned off: members can't set one
/// there, and nothing is announced. Quote the IDs, since YAML reads long
/// unquoted numbers as numbers, and some tools round them.
///
/// The configuration is read on every call, so adding or removing a server and
/// reloading with `SIGHUP` takes effect immediately.
///
/// @param guildId   the server
/// @param channelId the channel birthdays are announced in
public record Config(long guildId, long channelId, long roleId, String message) {
    private static final Setting<Map<String, Object>> CONFIG =
            Setting.map("plugins.birthdays", String.class, Object.class);
    private static final Logger LOG = LoggerFactory.getLogger(Config.class);
    private static final Set<String> KEYS = Set.of("channel", "role", "message");

    /// Returns every server that has birthdays turned on: the keys under
    /// `birthdays`. A key that isn't a guild ID is logged and skipped, so one
    /// typo doesn't hide `/birthday` from every other server.
    ///
    /// @param settings the bot's settings
    /// @return the guild IDs; empty if `birthdays` isn't configured
    public static Set<Long> guilds(Settings settings) {
        final var entries = settings.get(CONFIG).orElse(Map.of());
        final var out = new HashSet<Long>();
        for (var guild : entries.keySet()) {
            try {
                out.add(Long.parseUnsignedLong(guild.strip()));
            } catch (NumberFormatException ex) {
                LOG.warn("ignoring `plugins.birthdays` entry `{}': not a guild ID", guild);
            }
        }

        return Set.copyOf(out);
    }

    /// Looks up a server's configuration.
    /// @param settings the bot's settings
    /// @param guild    the server
    /// @return its configuration, or empty if birthdays are turned off there
    /// @throws dev.floofy.noel.SettingException if its entry is invalid, naming the entry's path
    public static Optional<Config> of(Settings settings, long guild) {
        return settings.get(Setting.of(String.format("plugins.birthdays['%d']".formatted(guild)), Object.class)
                .transform(raw -> {
                    final var channel = getChannel(raw);
                    final var role = getRole(raw);
                    final var message = getMessage(raw);

                    return new Config(guild, channel, role, message);
                }));
    }

    /// Whether birthdays are turned on in a server.
    /// @param settings the bot's settings
    /// @param guildId  the server
    /// @return whether the server has an entry under `birthdays`
    public static boolean isEnabled(Settings settings, long guildId) {
        return of(settings, guildId).isPresent();
    }

    private static long snowflake(Object raw) {
        return switch (raw) {
            case Long id -> id;
            case Integer id -> id.longValue();
            case String id -> Long.parseUnsignedLong(id);
            default -> throw new IllegalArgumentException(String.format("expected a Snowflake, got: %s", raw));
        };
    }

    private static long getChannel(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            throw new IllegalStateException("expected mapping");
        }

        for (var key : map.keySet()) {
            if (!(key instanceof String name && KEYS.contains(name))) {
                throw new IllegalArgumentException("unknown key `%s': expected `channel`".formatted(key));
            }
        }

        final var channel = map.get("channel");
        if (channel == null) {
            throw new IllegalArgumentException("missing `channel`");
        }

        return snowflake(channel);
    }

    private static long getRole(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            throw new IllegalStateException("expected mapping");
        }

        for (var key : map.keySet()) {
            if (!(key instanceof String name && KEYS.contains(name))) {
                throw new IllegalArgumentException("unknown key `%s': expected `role`".formatted(key));
            }
        }

        final var role = map.get("role");
        if (role == null) {
            throw new IllegalArgumentException("missing `role`");
        }

        return snowflake(role);
    }

    private static String getMessage(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            throw new IllegalStateException("expected mapping");
        }

        for (var key : map.keySet()) {
            if (!(key instanceof String name && KEYS.contains(name))) {
                throw new IllegalArgumentException("unknown key `%s': expected `message`".formatted(key));
            }
        }

        final var message = map.get("message");
        if (message != null && !(message instanceof String)) {
            throw new IllegalStateException("expected `string` for `message'");
        }

        return (String) message;
    }
}
