package dev.floofy.noel.modules.logging.composites;

import ch.qos.logback.classic.pattern.NamedConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class ThreadNameComposite extends NamedConverter {
    protected String getFullyQualifiedName(ILoggingEvent event) {
        final StringBuilder builder = new StringBuilder();

        // bracket
        builder.append('\033').append("[38;2;134;134;134m<").append('\033').append("[0m");

        // thread name
        builder.append('\033').append("[38;2;255;105;189m").append(event.getThreadName()).append('\033').append("[0m");

        // other bracket
        builder.append('\033').append("[38;2;134;134;134m>]").append('\033').append("[0m");

        return builder.toString();
    }
}
