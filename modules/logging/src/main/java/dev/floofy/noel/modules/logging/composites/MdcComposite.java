package dev.floofy.noel.modules.logging.composites;

import ch.qos.logback.classic.pattern.NamedConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.Map;

public class MdcComposite extends NamedConverter {
    @Override
    protected String getFullyQualifiedName(ILoggingEvent event) {
        final Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc.isEmpty()) return "";

        final StringBuilder builder = new StringBuilder();
        int idx = 0;

        for (Map.Entry<String, String> entry : mdc.entrySet()) {
            // More than 15 entries is probably too crazy to log!
            if (idx++ == 15) break;

            builder.append("\033[38;2;134;134;134m").append(entry.getKey()).append('=').append(entry.getValue()).append(' ').append("\033[0m");
        }

        return builder.toString();
    }
}
