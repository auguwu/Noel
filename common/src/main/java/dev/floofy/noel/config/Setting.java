/*
 * 🐾✨ Noel: Discord bot made to manage my servers, made in Java.
 * Copyright 2021-2023 Noel <cutie@floofy.dev>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.floofy.noel.config;

import java.util.Objects;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a configuration setting that be
 * @param <T> Setting type to use.
 */
public class Setting<T> {
    final Function<Object, T> identifySettingFn;
    final String key;

    /**
     * Creates a new {@link Setting<String>} object with the return value
     * being the raw value as a String.
     *
     * @param key The key to use. This can be in the form of dot-notation (<code>some.key.here</code>) for nested
     *            objects, it will be resolved by the Config to see if it exists.
     * @return {@link Setting<String>} to use.
     */
    public static Setting<String> string(@NotNull String key) {
        return new Setting<>(key, (value) -> {
            if (!(value instanceof String)) {
                throw new IllegalStateException("Value was not a String, received: " + value.getClass());
            }

            return (String) value;
        });
    }

    public static Setting<SecureSetting> secureSetting(@NotNull String key) {
        return new Setting<>(key, (value) -> {
            if (!(value instanceof String)) {
                throw new IllegalStateException("Value was not a String, received: " + value.getClass());
            }

            return new SecureSetting((String) value);
        });
    }

    protected Setting(@NotNull String key, @NotNull Function<Object, T> identifySettingFn) {
        this.identifySettingFn = Objects.requireNonNull(identifySettingFn, "missing identify resolver for setting");
        this.key = key;
    }

    public T resolve() {
        final Config config = Config.get();
        return config.get(this);
    }
}
