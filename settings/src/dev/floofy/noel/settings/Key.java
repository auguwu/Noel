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

package dev.floofy.noel.settings;

import java.util.*;

/// A path into nested [Map]s and [List]s, written in a JSONPath-like syntax.
///
/// ```java
/// var config = Map.of("hello", Map.of("world", "true"));
/// Key.of("hello.world").evaluate(config);                // Optional["true"]
/// Key.of("hello.world").evaluate(config, String.class);  // Optional["true"]
/// Key.of("hello.missing").evaluate(config);              // Optional.empty()
/// ```
///
/// ## Syntax
/// | Form          | Meaning                                            |
/// |---------------|----------------------------------------------------|
/// | `a.b`         | key `b` of the map at key `a`                      |
/// | `a[0]`        | element 0 of the list at key `a`                   |
/// | `a[-1]`       | last element of the list at key `a`                |
/// | `a['b.c']`    | key `b.c`, for keys containing `.`, `[` or `]`     |
/// | `a\.b`        | key `a.b`, escaping a single character             |
/// | `$.a`, `$a`   | a leading `$` root marker is accepted and ignored  |
///
/// Only single-value paths are supported: no wildcards, slices, filters or
/// recursive descent.
///
/// Keys are immutable and thread-safe. Parse once with [#of(String)] and reuse.
public final class Key {
    private sealed interface Segment permits Field, Index {}

    private record Field(String name) implements Segment {}

    private record Index(int index) implements Segment {}

    private final String path;
    private final List<Segment> segments;

    private Key(String path, List<Segment> segments) {
        this.path = path;
        this.segments = segments;
    }

    /// Parses a path.
    /// @param path the path, such as `hello.world` or `servers[0].name`
    /// @return the parsed key
    /// @throws IllegalArgumentException if the path is empty or malformed
    public static Key of(String path) {
        Objects.requireNonNull(path, "path");
        return new Key(path, Collections.unmodifiableList(parse(path)));
    }

    /// Resolves this key against `root`.
    ///
    /// Evaluation stops and returns empty as soon as a segment can't be followed:
    /// a missing key, a key mapped to `null`, an index out of range, or a segment
    /// applied to the wrong kind of value (an index on a map, a field on a list
    /// or a scalar).
    ///
    /// @param root the map to start from
    /// @return the value at this path, or empty if there is none
    public Optional<Object> evaluate(Map<String, ?> root) {
        Object current = Objects.requireNonNull(root, "root");
        for (var segment : this.segments) {
            current = step(current, segment);
            if (current == null) {
                return Optional.empty();
            }
        }

        return Optional.of(current);
    }

    /// Resolves this key against `root` and checks the result's type.
    ///
    /// @param root the map to start from
    /// @param type the expected type of the value
    /// @param <T>  the expected type
    /// @return the value at this path, or empty if there is none, or it is not a `T`.
    public <T> Optional<T> evaluate(Map<String, ?> root, Class<T> type) {
        return evaluate(root).filter(type::isInstance).map(type::cast);
    }

    /// Returns the path this key was parsed from.
    /// @return the original path
    public String path() {
        return path;
    }

    /// Follows one segment from `current`, or returns `null` if it can't be followed.
    private static Object step(Object current, Segment segment) {
        return switch (segment) {
            case Field(var name) when current instanceof Map<?, ?> map -> map.get(name);
            case Index(var index) when current instanceof List<?> list -> elementAt(list, index);
            default -> null;
        };
    }

    private static Object elementAt(List<?> list, int index) {
        var resolved = index < 0 ? list.size() + index : index;
        return resolved >= 0 && resolved < list.size() ? list.get(resolved) : null;
    }

    private static List<Segment> parse(String path) {
        var segments = new ArrayList<Segment>();
        var name = new StringBuilder();
        var n = path.length();
        var i = 0;

        // Optional root marker: "$", "$.a" or "$a".
        if (path.startsWith("$")) {
            i = path.startsWith("$.") ? 2 : 1;
        }

        if (i >= n) {
            throw malformed(path, i, "path is empty");
        }

        // True when the previous segment ended with ']', so a following '.' is allowed
        // without a name before it ("a[0].b").
        var afterBracket = false;

        while (i < n) {
            var c = path.charAt(i);
            switch (c) {
                case '.' -> {
                    if (!name.isEmpty()) {
                        segments.add(new Field(name.toString()));
                        name.setLength(0);
                    } else if (!afterBracket) {
                        throw malformed(path, i, "empty segment");
                    }

                    if (i == n - 1) {
                        throw malformed(path, i, "path ends with '.'");
                    }

                    afterBracket = false;
                    i++;
                }

                case '[' -> {
                    if (!name.isEmpty()) {
                        segments.add(new Field(name.toString()));
                        name.setLength(0);
                    }

                    i = parseBracket(path, i, segments);
                    afterBracket = true;
                }

                case ']' -> throw malformed(path, i, "unexpected ']'");

                case '\\' -> {
                    if (i == n - 1) {
                        throw malformed(path, i, "dangling escape");
                    }

                    name.append(path.charAt(i + 1));
                    afterBracket = false;
                    i += 2;
                }

                default -> {
                    if (afterBracket) {
                        throw malformed(path, i, "expected '.' or '[' after ']'");
                    }

                    name.append(c);
                    i++;
                }
            }
        }

        if (!name.isEmpty()) {
            segments.add(new Field(name.toString()));
        }

        return segments;
    }

    /// Parses `[0]`, `[-1]`, `['name']` or `["name"]` starting at `open`.
    ///
    /// @return the index just past the closing `]`
    private static int parseBracket(String path, int open, List<Segment> segments) {
        var start = open + 1;
        if (start >= path.length()) {
            throw malformed(path, open, "unclosed '['");
        }

        var quote = path.charAt(start);
        if (quote == '\'' || quote == '"') {
            var name = new StringBuilder();
            var i = start + 1;
            while (i < path.length() && path.charAt(i) != quote) {
                if (path.charAt(i) == '\\' && i + 1 < path.length()) {
                    i++;
                }

                name.append(path.charAt(i));
                i++;
            }

            if (i + 1 >= path.length() || path.charAt(i + 1) != ']') {
                throw malformed(path, open, "unclosed quoted key");
            }

            segments.add(new Field(name.toString()));
            return i + 2;
        }

        var close = path.indexOf(']', start);
        if (close < 0) {
            throw malformed(path, open, "unclosed '['");
        }

        var inner = path.substring(start, close).strip();
        try {
            segments.add(new Index(Integer.parseInt(inner)));
        } catch (NumberFormatException ex) {
            throw malformed(path, open, "expected an index or a quoted key, got '" + inner + "'");
        }

        return close + 1;
    }

    private static IllegalArgumentException malformed(String path, int at, String reason) {
        return new IllegalArgumentException(String.format("malformed key [%s] at offset %d: %s", path, at, reason));
    }

    @Override
    public boolean equals(Object that) {
        return that instanceof Key other && segments.equals(other.segments);
    }

    @Override
    public int hashCode() {
        return segments.hashCode();
    }

    @Override
    public String toString() {
        return "Key[" + path + "]";
    }
}

/*
public final class Key {

}
 */
