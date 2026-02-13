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

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Properties;

public final class BuildInfo {
    private static final Properties properties = new Properties();
    private static final long startedAt = System.currentTimeMillis();

    private static final String VERSION_KEY = "noel.version";
    private static final String GIT_COMMIT_KEY = "noel.git.commit";
    private static final String BUILD_TIMESTAMP_KEY = "noel.build.timestamp";
    private static final String BUILD_DATA_PROPERTIES = "/bot/build-data.properties";

    private BuildInfo() {}

    private static void loadProperties() {
        if (!properties.isEmpty()) {
            return;
        }

        try (var stream = BuildInfo.class.getResourceAsStream(BUILD_DATA_PROPERTIES)) {
            if (stream == null) {
                System.err.println("FATAL: Couldn't find `" + BUILD_DATA_PROPERTIES + "' in classpath, should never happen, using default values instead");
                return;
            }

            properties.load(stream);
        } catch(IOException ex) {
            System.err.println("FATAL: tried to read from `" + BUILD_DATA_PROPERTIES + "` but failed");
            ex.printStackTrace();
            System.exit(128);
        }
    }

    @NotNull
    private static String getProperty(@NotNull String key) {
        loadProperties();

        var value = properties.get(key);
        if (value == null) {
            throw new RuntimeException(String.format("failed to find property '%s'", key));
        }

        assert value instanceof String : "value was not a string";

        var val = value.toString();
        return val.substring(1, val.length() - 1); // removes any quotes (`"`)
    }

    /**
     * Returns the current version of this build.
     */
    @NotNull
    public static String getVersion() {
        return getProperty(VERSION_KEY);
    }

    @NotNull
    public static String getGitCommit() {
        return getProperty(GIT_COMMIT_KEY);
    }

    @NotNull
    public static String getBuildTimestamp() {
        return getProperty(BUILD_TIMESTAMP_KEY);
    }

    public static long getStartedAt() {
        return startedAt;
    }
}
