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
