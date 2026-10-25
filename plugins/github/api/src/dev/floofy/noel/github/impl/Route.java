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

package dev.floofy.noel.github.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import okhttp3.HttpUrl;

/// A parsed route template, such as `/repos/:owner/:repo/issues`.
///
/// ## Syntax
/// - `:name` is replaced by one path segment. The value is percent-encoded, so
///   a `/` in it stays part of the segment.
/// - `*name` is replaced by the rest of the path, slashes included, for routes
///   like `/repos/:owner/:repo/contents/*path`. It must be the last segment.
/// - Anything else is literal.
///
/// @param template   the template as written
/// @param segments   its parsed segments
/// @param parameters the names of its parameters, in order
record Route(String template, List<Segment> segments, Set<String> parameters) {
    sealed interface Segment {}

    record Literal(String text) implements Segment {}

    record Parameter(String name) implements Segment {}

    record Rest(String name) implements Segment {}

    /// Parses a template.
    /// @param template the template, starting with `/`
    /// @throws IllegalArgumentException if the template is malformed
    static Route parse(String template) {
        if (!template.startsWith("/")) {
            throw new IllegalArgumentException("route `%s' must start with `/'".formatted(template));
        }

        if (template.indexOf('?') >= 0) {
            throw new IllegalArgumentException(
                    "route `%s' has a query string; use GitHubRequest.query(...) instead".formatted(template));
        }

        final var parts = template.substring(1).split("/", -1);
        final var segments = new ArrayList<Segment>(parts.length);
        final var parameters = new LinkedHashSet<String>();

        for (int i = 0; i < parts.length; i++) {
            final var part = parts[i];
            if (part.isEmpty()) {
                throw new IllegalArgumentException("route `%s' has an empty segment".formatted(template));
            }

            final Segment segment = switch (part.charAt(0)) {
                case ':' -> new Parameter(name(template, part));
                case '*' -> {
                    if (i != parts.length - 1) {
                        throw new IllegalArgumentException(
                                "route `%s': `%s' must be the last segment".formatted(template, part));
                    }

                    yield new Rest(name(template, part));
                }
                default -> new Literal(part);
            };

            switch (segment) {
                case Parameter(var name) -> addParameter(template, parameters, name);
                case Rest(var name) -> addParameter(template, parameters, name);
                case Literal literal -> {}
            }

            segments.add(segment);
        }

        return new Route(template, List.copyOf(segments), Set.copyOf(parameters));
    }

    /// Appends this route's path to `url`, filling in its parameters.
    /// @param url    the base URL to append to
    /// @param values a value for every parameter; values are converted with `String.valueOf`
    /// @throws IllegalArgumentException if a parameter has no value, or `values`
    ///         has a key the route doesn't use, which is usually a typo
    HttpUrl.Builder resolve(HttpUrl.Builder url, Map<String, ?> values) {
        for (var key : values.keySet()) {
            if (!parameters.contains(key)) {
                throw new IllegalArgumentException(
                        "route `%s' has no parameter `%s'; it has %s".formatted(template, key, parameters));
            }
        }

        for (var segment : segments) {
            switch (segment) {
                case Literal(var text) -> url.addPathSegment(text);
                case Parameter(var name) -> url.addPathSegment(value(values, name));
                case Rest(var name) -> url.addPathSegments(value(values, name));
            }
        }

        return url;
    }

    private String value(Map<String, ?> values, String name) {
        final var value = values.get(name);
        if (value == null) {
            throw new IllegalArgumentException("route `%s' needs a value for `%s'".formatted(template, name));
        }

        final var text = String.valueOf(value);
        if (text.isEmpty()) {
            throw new IllegalArgumentException("route `%s': `%s' is empty".formatted(template, name));
        }

        return text;
    }

    private static String name(String template, String part) {
        final var name = part.substring(1);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("route `%s' has a parameter with no name".formatted(template));
        }

        return name;
    }

    private static void addParameter(String template, Set<String> parameters, String name) {
        if (!parameters.add(name)) {
            throw new IllegalArgumentException("route `%s' uses `%s' twice".formatted(template, name));
        }
    }
}
