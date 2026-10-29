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

import java.util.*;
import java.util.stream.Collectors;

/// The GitHub events Noel can post, named as GitHub names them in the
/// `X-GitHub-Event` header and in the webhook settings page.
///
/// A webhook with no events chosen posts the [default][#isDefault()] ones.
/// GitHub only sends the events selected in its own settings, so a webhook here
/// can only post events that are selected in both places.
public enum Event {
    PUSH("push", "Commits pushed to a branch", true),
    PULL_REQUEST("pull_request", "Pull requests opened, closed, merged or reopened", true),
    ISSUES("issues", "Issues opened, closed or reopened", true),
    ISSUE_COMMENT("issue_comment", "New comments on issues and pull requests", false),
    RELEASE("release", "Releases published", true),
    CREATE("create", "Branches and tags created", false),
    DELETE("delete", "Branches and tags deleted", false),
    STAR("star", "Someone starred the repository", false),
    FORK("fork", "Someone forked the repository", false),
    WORKFLOW_RUN("workflow_run", "GitHub Actions runs that finished", false);

    private final String id;
    private final String description;
    private final boolean isDefault;

    Event(String id, String description, boolean isDefault) {
        this.id = id;
        this.description = description;
        this.isDefault = isDefault;
    }

    /// Returns GitHub's name for the event, such as `pull_request`.
    /// @return the name
    public String id() {
        return id;
    }

    /// Returns a one-line description, for `/webhooks events`.
    /// @return the description
    public String description() {
        return description;
    }

    /// Whether the event is posted by a webhook with no events chosen.
    /// @return whether it's a default
    public boolean isDefault() {
        return isDefault;
    }

    /// Looks up an event by GitHub's name, ignoring case.
    /// @param id the name, such as `push`
    /// @return the event, or empty if Noel doesn't support it
    public static Optional<Event> byId(String id) {
        final var normalized = id.strip().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(event -> event.id.equals(normalized))
                .findFirst();
    }

    /// Returns the default events' names.
    /// @return the names, in declaration order
    public static List<String> defaults() {
        return Arrays.stream(values()).filter(Event::isDefault).map(Event::id).toList();
    }

    /// Parses a comma-separated list such as `push, issues`. `default` (or an
    /// empty list) means the defaults, returned as an empty set.
    ///
    /// @param input what the user typed
    /// @return the event names, in the order given
    /// @throws IllegalArgumentException naming every unknown event
    public static Set<String> parseList(String input) throws IllegalArgumentException {
        final var names = Arrays.stream(input.split(","))
                .map(String::strip)
                .filter(name -> !name.isEmpty())
                .map(name -> name.toLowerCase(Locale.ROOT))
                .toList();

        if (names.isEmpty() || names.equals(List.of("default"))) {
            return Set.of();
        }

        final var unknown = names.stream().filter(name -> byId(name).isEmpty()).toList();
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("unknown event(s): %s. Supported: %s"
                    .formatted(
                            String.join(", ", unknown),
                            Arrays.stream(values()).map(Event::id).collect(Collectors.joining(", "))));
        }

        return new LinkedHashSet<>(names);
    }
}
