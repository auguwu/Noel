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

package dev.floofy.noel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;

public class NoelInfo {
    private static NoelInfo instance;
    private String commitHash;
    private String buildDate;
    private String version;

    public static NoelInfo getInstance() {
        if (instance != null) {
            return instance;
        }

        final NoelInfo info = new NoelInfo();
        info.init();

        instance = info;
        return instance;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public String getBuildDate() {
        return buildDate;
    }

    public String getVersion() {
        return version;
    }

    private void init() {
        if (instance != null) {
            return;
        }

        try (final InputStream is = getClass().getResourceAsStream("/build-info.json")) {
            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode node = mapper.readValue(is, JsonNode.class);

            commitHash = node.get("commit.sha").asText();
            buildDate = node.get("build.date").asText();
            version = node.get("version").asText();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
