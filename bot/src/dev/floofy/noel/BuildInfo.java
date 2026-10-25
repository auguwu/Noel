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
import java.util.Objects;
import java.util.Properties;

/// Version and build information for this build of Noel.
///
/// The values come from `build-data.properties`, which the `//bot:build_data` Bazel genrule
/// writes from the Bazel's workspace status at build time.
///
/// The file is read lazily, on the first call to an accessor. If it is missing
/// or unreadable, the build is broken rather than misconfigured, so the process
/// exits immediately instead of throwing.
public final class BuildInfo {
    private static final Properties properties = new Properties();
    private static final long bootstrappedAt = System.currentTimeMillis();

    private static final String VERSION_KEY = "noel.version";
    private static final String GIT_COMMIT_KEY = "noel.git.commit";
    private static final String BUILD_TIMESTAMP_KEY = "noel.build.timestamp";
    private static final String BUILD_DATA_PROPERTIES = "/bot/build-data.properties";

    private BuildInfo() {}

    /// Returns the version Noel was built as.
    /// @throws RuntimeException if the build data has no version
    public static String getVersion() {
        return getProperty(VERSION_KEY);
    }

    /// Returns the Git commit Noel was built from.
    /// @throws RuntimeException if the build data has no commit
    public static String getGitCommit() {
        return getProperty(GIT_COMMIT_KEY);
    }

    /// Returns when this binary was built.
    /// @return an ISO-8601 instant, such as `2026-10-24T19:13:00Z`, from
    ///         `NOEL_BUILD_TIMESTAMP`; parse it with [java.time.Instant#parse(CharSequence)]
    /// @throws RuntimeException if the build data has no timestamp
    public static String getBuildTimestamp() {
        return getProperty(BUILD_TIMESTAMP_KEY);
    }

    /// Returns when this class was first used, as a stand-in for when Noel
    /// started. [dev.floofy.Noel] reads the build data before anything
    /// else, so in practice this is within milliseconds of process start.
    ///
    /// @return milliseconds since the Unix epoch, as from [System#currentTimeMillis()]
    public static long getStartedAt() {
        return bootstrappedAt;
    }

    private static String getProperty(String key) {
        Objects.requireNonNull(key, "key");
        loadProperties();

        var value = properties.get(key);
        if (value == null) {
            throw new RuntimeException(String.format("failed to find property '%s'", key));
        }

        assert value instanceof String : "value was not a string";

        var val = value.toString();
        if (val.startsWith("\"") && val.endsWith("\"")) {
            return val.substring(1, val.length() - 1); // removes any quotes (`"`)
        } else {
            return val;
        }
    }

    private static void loadProperties() {
        if (!properties.isEmpty()) {
            return;
        }

        try (var stream = BuildInfo.class.getResourceAsStream(BUILD_DATA_PROPERTIES)) {
            if (stream == null) {
                System.err.printf("FATAL: cannot find `%s` in classpath%n", BUILD_DATA_PROPERTIES);
                System.exit(1);
            }

            properties.load(stream);
        } catch (IOException ex) {
            System.err.printf("FATAL: cannot read properties file from `%s`%n", BUILD_DATA_PROPERTIES);
            ex.printStackTrace();

            System.exit(128);
        }
    }
}
