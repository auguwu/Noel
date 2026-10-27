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

package dev.floofy.noel.discord;

import dev.floofy.noel.Setting;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.dv8tion.jda.api.entities.Activity;

/// One presence the bot rotates through.
///
/// Configured as a list under `plugins.discord.presences`. Each entry is
/// either a plain string, shown as a custom status, or a map with a `message`
/// and an optional `status`:
///
/// ```yaml
/// modules:
///   discord:
///     presenceMessages:
///       - "hi world"            # custom status: shows exactly "hi world"
///       - message: "you"
///         status: watching      # shows "Watching you"
///       - message: "with yarn"
///         status: playing       # shows "Playing with yarn"
/// ```
///
/// `status` is one of `custom` (the default), `playing`, `listening`, `watching`
/// or `competing`, ignoring case. `streaming` isn't supported, because Discord
/// needs a stream URL for it.
public final class PresenceMeta {
    /// Discord's limit on an activity's text.
    private static final int MAX_LENGTH = 128;
    private static final Set<String> KEYS = Set.of("message", "activity");

    private final String message;
    private final Activity.ActivityType type;

    PresenceMeta(String message, Activity.ActivityType type) {
        this.message = Objects.requireNonNull(message, "message");
        this.type = Objects.requireNonNull(type, "type");
    }

    /// Reads one list entry: a string, or a map with `message` and `status`.
    ///
    /// Problems are reported with the entry's path, for example
    /// `setting [modules.discord.presenceMessages[1]] is invalid: unknown status `wathcing'`.
    ///
    /// @param path the entry's path
    /// @return the setting for that entry
    public static Setting<PresenceMeta> setting(String path) {
        return Setting.of(path, Object.class).transform(raw -> switch (raw) {
            case String message -> create(message, Activity.ActivityType.CUSTOM_STATUS);
            case Map<?, ?> map -> fromMap(map);
            default -> throw new IllegalArgumentException("expected a string, or a map with `message` and `status`");
        });
    }

    public Activity.ActivityType type() {
        return type;
    }

    public String message() {
        return message;
    }

    /// Builds the JDA activity. A custom status puts the text in `state`, which
    /// is where Discord shows it; [Activity#of] would put it in `name` instead.
    ///
    /// @return the activity to set
    public Activity toActivity() {
        return type == Activity.ActivityType.CUSTOM_STATUS
                ? Activity.customStatus(this.message)
                : Activity.of(type, message);
    }

    @Override
    public String toString() {
        return "PresenceMeta[" + type + ": " + message + "]";
    }

    private static PresenceMeta fromMap(Map<?, ?> map) {
        Objects.requireNonNull(map, "map");

        for (var key : map.keySet()) {
            if (!(key instanceof String name && KEYS.contains(name))) {
                throw new IllegalArgumentException(
                        "unknown key `%s'; expected `message` and optionally `status`".formatted(key));
            }
        }

        if (!(map.get("message") instanceof String message)) {
            throw new IllegalArgumentException("`message` must be a string");
        }

        final var status = map.get("activity");
        if (status == null) {
            return create(message, Activity.ActivityType.CUSTOM_STATUS);
        }

        if (!(status instanceof String name)) {
            throw new IllegalArgumentException("`activity` must be a string");
        }

        return create(message, parseType(name));
    }

    private static PresenceMeta create(String message, Activity.ActivityType type) {
        final var text = message.strip();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("message is empty");
        }

        final int length = text.codePointCount(0, text.length());
        if (length > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "message is %d characters; Discord allows at most %d".formatted(text.length(), MAX_LENGTH));
        }

        return new PresenceMeta(text, type);
    }

    private static Activity.ActivityType parseType(String name) {
        final var normalized = name.strip().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "CUSTOM", "CUSTOM_STATUS" -> Activity.ActivityType.CUSTOM_STATUS;
            case "PLAYING" -> Activity.ActivityType.PLAYING;
            case "LISTENING" -> Activity.ActivityType.LISTENING;
            case "WATCHING" -> Activity.ActivityType.WATCHING;
            case "COMPETING" -> Activity.ActivityType.COMPETING;
            case "STREAMING" ->
                throw new IllegalArgumentException("`streaming` isn't supported, since it needs a stream URL");
            default ->
                throw new IllegalArgumentException(
                        "unknown status `%s'; expected custom, playing, listening, watching or competing"
                                .formatted(name));
        };
    }
}
