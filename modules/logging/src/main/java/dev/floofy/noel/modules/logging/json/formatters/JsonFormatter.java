package dev.floofy.noel.modules.logging.json.formatters;

import java.util.Map;

/**
 * Represents an interface for formatting a Map object into a JSON string.
 */
public interface JsonFormatter {
    /**
     * Formats the given {@link Map} to return a JSON string.
     * @param data The data that is given by {@link org.noelware.charted.modules.logging.json.JsonLayout}
     * @return JSON string
     * @throws Exception If anything occurred while transforming.
     */
    String doFormat(Map<String, Object> data) throws Exception;
}
