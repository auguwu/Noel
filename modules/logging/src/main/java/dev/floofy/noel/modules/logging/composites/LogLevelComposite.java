package dev.floofy.noel.modules.logging.composites;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.color.ANSIConstants;
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase;

public class LogLevelComposite extends ForegroundCompositeConverterBase<ILoggingEvent> {
    @Override
    protected String getForegroundColorCode(ILoggingEvent event) {
        final Level level = event.getLevel();
        return switch (level.toInt()) {
            case Level.TRACE_INT -> ANSIConstants.BOLD + "38;2;156;156;252";
            case Level.DEBUG_INT -> ANSIConstants.BOLD + "38;2;163;182;138";
            case Level.ERROR_INT -> ANSIConstants.BOLD + "38;2;153;75;104";
            case Level.WARN_INT -> ANSIConstants.BOLD + "38;2;243;243;134";
            case Level.INFO_INT -> ANSIConstants.BOLD + "38;2;178;157;243";
            default -> ANSIConstants.DEFAULT_FG;
        };
    }
}
