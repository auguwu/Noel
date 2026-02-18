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

import java.util.function.Function;

public final class Setting<T> {
    private final Function<Object, T> converter;
    private final String name;

    private Setting(@NotNull String name, Function<Object, T> converter) {
        this.converter = converter;
        this.name = name;
    }

    public static <T> Setting<T> of(@NotNull String key, Function<Object, T> converter) {
        return new Setting<>(key, converter);
    }

    public static Setting<String> string(@NotNull String key) {
        return Setting.string(key, false);
    }

    public static Setting<String> string(@NotNull String key, boolean required) {
        return Setting.of(key, (value) -> {
            if (required && value == null) {
                throw new IllegalStateException(String.format("Expected configuration key `%s' to be present", key));
            }

            if (!(value instanceof String)) {
                throw new IllegalStateException(String.format("Expected configuration value '%s' to be of string, received [%s] instead", key, value.getClass()));
            }

            return (String)value;
        });
    }

    public Function<Object, T> getConverter() {
        return converter;
    }

    public String getName() {
        return name;
    }
}
