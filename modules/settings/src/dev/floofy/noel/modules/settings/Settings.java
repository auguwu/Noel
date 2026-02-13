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
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public final class Settings {
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    private Map<String, Object> settings;

    public void initialize() {
        var env = System.getenv("NOEL_CONFIG_PATH");
        final var path = new File(env != null ? env : "./config.yaml");
        if (!path.exists()) {
            throw new IllegalStateException("`./config.yaml` doesn't exist :(");
        }

        if (!path.isFile()) {
            throw new IllegalStateException("`./config.yaml` must be a file");
        }

        //noinspection unchecked
        settings = Collections.unmodifiableMap((Map<String, Object>)MAPPER.readValue(path, Map.class));
    }

    @Nullable
    public <T> T get(@NotNull Setting<T> setting) {
        Objects.requireNonNull(settings);

        var raw = JSONPath.from(settings, setting.getName());
        if (raw.isEmpty()) {
            return null;
        }

        return setting.getConverter().apply(raw.get());
    }

    @NotNull
    public <T> T getOrDefault(@NotNull Setting<T> setting, T defaultValue) {
        var value = get(setting);
        if (value == null) {
            return defaultValue;
        }

        return value;
    }
}
