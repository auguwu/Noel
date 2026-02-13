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

package dev.floofy.noel.modules.settings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/// Minimal JSONPath-like evaluation for nested [Map] and [List] structures.
///
/// ## Features
/// * Dot notation for map-like keys: `foo.bar`
/// * Bracket notation for lists: `arr[0]`
/// * Supports nested lists and maps: `foo.list[2].nested`
/// * Safe [Optional] return when a key or index doesn't exist.
///
/// ## Remarks
/// This doesn't implement full JSONPath features like wildcards, filters, or
/// recursive descent.
public final class JSONPath {
    private JSONPath() {}

    /// Resolves a JSONPath-style pointer on a nested [Map] or [List] structure.
    /// @param root The root object, typically a [Map] representing parsed JSON/TOML.
    /// @param pointer Path string.
    /// @return The value itself or nothing at all.
    /// @throws IllegalStateException if a list index is invalid or bracket notation is misused.
    public static Optional<Object> from(@NotNull Object root, @NotNull String pointer) {
        if (pointer.isEmpty() || pointer.equals(".")) {
            return Optional.empty();
        }

        Object current = root;
        for (var token: pointer.split("\\.")) {
            current = resolveToken(current, token);
            if (current == null) {
                return Optional.empty();
            }
        }

        return Optional.of(current);
    }

    private static Object resolveToken(@Nullable Object current, @NotNull String token) {
        if (current instanceof Map<?, ?> map) {
            if (token.contains("[") || token.contains("]")) {
                throw new IllegalStateException(String.format("Token \"%s\" cannot contain brackets for a Map", token));
            }

            return map.get(token);
        } else if (current instanceof List<?> list) {
            return resolveListToken(list, token);
        }

        return null;
    }

    private static Object resolveListToken(@NotNull List<?> list, @NotNull String token) {
        String remaining = token;
        Object current = list;

        while (remaining.contains("[")) {
            int first = remaining.indexOf('[');
            int last = remaining.indexOf(']');
            if (first < 0 || last < 0 || last < first) {
                throw new IllegalStateException(String.format("Invalid list token: %s", token));
            }

            int index;
            try {
                index = Integer.parseInt(remaining.substring(first+1, last));
            } catch (NumberFormatException e) {
                throw new IllegalStateException(String.format("invalid array index in token \"%s\"", token), e);
            }

            List<?> currList = (List<?>) current;
            if (index < 0 || index >= currList.size()) {
                return null;
            }

            current = currList.get(index);
            remaining = remaining.substring(last + 1);
        }

        if (!remaining.isEmpty()) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(remaining);
            } else {
                return null;
            }
        }

        return current;
    }
}