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

package dev.floofy.noel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

/// A loaded configuration file that [Setting]s are read from.
///
/// ```java
/// var settings = Settings.load();
///
/// String token = settings.require(Setting.string("discord.token"));
/// int shards = settings.get(Setting.integer("discord.shards")).orElse(1);
/// List<String> owners = settings.require(Setting.list("discord.owners", Setting::string));
/// ```
///
/// Settings is an ordinary object rather than static state, so it can be bound in
/// the core injector and injected wherever it's needed, and tests can build one
/// from a map with [#of(Map)].
///
/// ## Reloading
///
/// [#reload()] re-reads the file and swaps the configuration in atomically, then
/// notifies every listener registered with [#onReload(Runnable)]. Each read sees
/// either the old or the new configuration, never a mix, but two separate reads
/// can straddle a reload. Read related settings once and keep the values if they
/// must agree with each other.
public final class Settings {
    /// Environment variable naming the configuration file to load.
    public static final String ENV_VAR = "NOEL_CONFIG_PATH";

    /// Locations checked, in order, when [#ENV_VAR] is not set.
    public static final List<Path> DEFAULT_LOCATIONS = List.of(Path.of("config", "noel.yaml"), Path.of("config.yaml"));

    private static final TypeReference<Map<String, Object>> ROOT_TYPE = new TypeReference<>() {};
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());
    private static final Logger LOG = LoggerFactory.getLogger(Settings.class);

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private volatile Map<String, Object> root;
    private final Path source;

    private Settings(Path source, Map<String, Object> root) {
        this.source = source;
        this.root = Objects.requireNonNull(root, "root");
    }

    /// Loads the configuration file named by `$NOEL_CONFIG_PATH`, or else the first
    /// of [#DEFAULT_LOCATIONS] that exists.
    ///
    /// @return the loaded settings
    /// @throws IllegalStateException if no configuration file can be found, or the
    ///         path named by `$NOEL_CONFIG_PATH` is not a regular file
    /// @throws IOException if the file cannot be read or is not valid YAML
    public static Settings load() throws IOException, IllegalStateException {
        var fromEnv = System.getenv(ENV_VAR);
        if (fromEnv != null && !fromEnv.isBlank()) {
            var path = Path.of(fromEnv);
            if (!Files.isRegularFile(path)) {
                throw new IllegalStateException(
                        String.format("$%s points to [%s], which is not a file", ENV_VAR, path));
            }

            return load(path);
        }

        for (var path : DEFAULT_LOCATIONS) {
            if (Files.isRegularFile(path)) {
                return load(path);
            }
        }

        throw new IllegalStateException(String.format(
                "unable to find configuration file: set the `$%s` environment variable or create `./config/noel.yaml`"
                        + " or `./config.yaml` in the root directory",
                ENV_VAR));
    }

    /// Loads a specific YAML configuration file.
    /// @param path the file to load
    /// @return the loaded settings, which can be [reloaded][#reload()]
    /// @throws IOException if the file cannot be read or is not valid YAML
    public static Settings load(Path path) throws IOException {
        return new Settings(path, read(path));
    }

    /// Wraps an existing map, for tests or configuration built in code. These
    /// settings have no file, so they cannot be reloaded.
    ///
    /// @param root the configuration
    /// @return the settings
    public static Settings of(Map<String, ?> root) {
        return new Settings(null, Map.copyOf(Objects.requireNonNull(root, "root")));
    }

    /// Re-reads the configuration file and swaps it in, then notifies every
    /// listener registered with [#onReload(Runnable)], in registration order.
    ///
    /// If the file can't be read or parsed, the current configuration is kept and
    /// no listener runs. A listener that throws is logged and does not stop the
    /// others.
    ///
    /// @throws IllegalStateException if these settings were not loaded from a file
    /// @throws IOException if the file cannot be read or is not valid YAML
    public void reload() throws IOException, IllegalStateException {
        if (this.source == null) {
            throw new IllegalStateException("settings created with `Settings.of(...)` have no file to reload");
        }

        this.root = Collections.unmodifiableMap(read(this.source));
        LOG.info("reloaded configuration successfully");

        for (var listener : listeners) {
            try {
                listener.run();
            } catch (RuntimeException ex) {
                LOG.error("configuration reload listener failed:", ex);
            }
        }
    }

    /// Registers `listener` to run after every successful [#reload()].
    /// @param listener runs on the thread that called [#reload()]
    /// @return a handle that unregisters the listener when closed
    public AutoCloseable onReload(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    /// Reads a setting.
    /// @param setting the setting to read
    /// @param <T>     the setting's type
    /// @return the value, or empty if the setting is missing
    /// @throws SettingException if the value is present but invalid
    public <T> Optional<T> get(Setting<T> setting) {
        return setting.get(root);
    }

    /// Reads a setting, requiring it to be present.
    /// @param setting the setting to read
    /// @param <T>     the setting's type
    /// @throws SettingException if the setting is missing or invalid
    public <T> T require(Setting<T> setting) {
        return setting.require(root);
    }

    /// Reads a setting, falling back to `fallback` if it is missing.
    /// @param setting  the setting to read
    /// @param fallback the value to use if the setting is missing
    /// @param <T>      the setting's type
    /// @return the value, or `fallback`
    /// @throws SettingException if the value is present but invalid
    public <T> T getOrDefault(Setting<T> setting, T fallback) {
        return setting.getOrDefault(root, fallback);
    }

    /// Returns the raw configuration map, as of now.
    /// @return an unmodifiable view of the top-level map
    public Map<String, Object> getRootMapping() {
        return root;
    }

    private static Map<String, Object> read(Path path) throws IOException {
        Map<String, Object> root;
        try (var in = Files.newInputStream(path)) {
            root = MAPPER.readValue(in, ROOT_TYPE);
        }

        return root != null ? root : Map.of();
    }
}
