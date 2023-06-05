package dev.floofy.noel.modules.logging.json.formatters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

public class DefaultJsonFormatter implements JsonFormatter {
    private final ObjectMapper MAPPER = new ObjectMapper();
    private boolean isPrettyPrint = false;

    /**
     * Returns whether pretty printing should be enabled. Enabled by default,
     * can be disabled with the <code>charted.json.pretty-print</code> configuration
     * key.
     */
    public boolean isPrettyPrintEnabled() {
        return isPrettyPrint;
    }

    /**
     * Sets the value for whether pretty printing should be enabled.
     * @param value The value.
     */
    public void setIsPrettyPrint(boolean value) {
        this.isPrettyPrint = value;
    }

    @Override
    public String doFormat(Map<String, Object> data) throws Exception {
        final Writer writer = new StringWriter(512);
        final JsonGenerator generator = MAPPER.getFactory().createGenerator(writer);

        if (isPrettyPrintEnabled()) {
            generator.useDefaultPrettyPrinter();
        }

        MAPPER.writeValue(writer, data);
        writer.flush();

        return writer.toString() + "\n";
    }
}
