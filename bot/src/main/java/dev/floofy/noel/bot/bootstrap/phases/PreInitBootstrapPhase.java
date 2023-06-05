package dev.floofy.noel.bot.bootstrap.phases;

import dev.floofy.noel.NoelInfo;
import dev.floofy.noel.bot.bootstrap.BootstrapPhase;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.floofy.utils.kotlin.FormatExtensionsKt;

import java.io.IOError;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class PreInitBootstrapPhase implements BootstrapPhase {
    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMM dd, yyyy @ hh:mm:ss z");
    private static final Map<Function<Object, Boolean>, Integer> CODES = new HashMap<>();
    static {
        CODES.put((i) -> i instanceof InternalError, 128);
        CODES.put((i) -> i instanceof OutOfMemoryError, 127);
        CODES.put((i) -> i instanceof StackOverflowError, 126);
        CODES.put((i) -> i instanceof UnknownError, 125);
        CODES.put((i) -> i instanceof IOError, 124);
        CODES.put((i) -> i instanceof LinkageError, 123);
    }

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    // credit: https://github.com/elastic/logstash/blob/main/logstash-core/src/main/java/org/logstash/Logstash.java#L98-L133
    private void installDefaultThreadExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            if (ex instanceof Error) {
                LOG.error("Uncaught fatal exception occurred in thread [{} ({})]:", thread.getName(), thread.getId(), ex);

                for (Map.Entry<Function<Object, Boolean>, Integer> entries : CODES.entrySet()) {
                    final var func = entries.getKey();
                    final int code = entries.getValue();

                    if (func.apply(ex)) {
                        Runtime.getRuntime().halt(code);
                    }
                }

                System.exit(1);
            } else {
                LOG.error("Uncaught exception occurred on thread [{} ({})]", thread.getName(), thread.getId(), ex);
            }
        });
    }

    @Override
    public void bootstrap() {
        installDefaultThreadExceptionHandler();

        final Runtime runtime = Runtime.getRuntime();
        runtime.addShutdownHook(new ShutdownPhaseThread());

        final OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        final NoelInfo info = NoelInfo.getInstance();

        LOG.info("Initializing Noel v{}+{} ~ built at {}",
            info.getVersion(),
            info.getCommitHash(),
            DATE_FORMAT.format(Date.from(Instant.parse(info.getBuildDate()))));

        LOG.info("~> Memory: {}/{}", FormatExtensionsKt.sizeToStr(runtime.totalMemory(), false), FormatExtensionsKt.sizeToStr(runtime.freeMemory(), false));
        LOG.info("~> Java:   v{} ({})", System.getProperty("java.version"), System.getProperty("java.vendor"));
        LOG.info("~> OS:     {} on {} ({} processors)", os.getName(), os.getArch(), os.getAvailableProcessors());
    }

    @Override
    @NotNull
    public String mdcValue() {
        return "preinit";
    }
}
