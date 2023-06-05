package dev.floofy.noel.bot;

import dev.floofy.noel.bot.bootstrap.BootstrapPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class Bootstrap {
    private static final Logger LOG = LoggerFactory.getLogger(Bootstrap.class);
    public static void main(String[] args) throws Exception {
        Thread.currentThread().setName("Noel-BootstrapThread");

        for (BootstrapPhase phase : BootstrapPhase.PHASES) {
            MDC.put("bootstrap.phase", phase.mdcValue());
            phase.bootstrap();

            // When JDA launches in the configure modules phase, we remove it so any logs
            // from JDA won't have the "bootstrap.phase" MDC property.
            if (MDC.getCopyOfContextMap() != null && MDC.getCopyOfContextMap().containsKey("bootstrap.phase")) {
                MDC.remove("bootstrap.phase");
            }
        }
    }
}
