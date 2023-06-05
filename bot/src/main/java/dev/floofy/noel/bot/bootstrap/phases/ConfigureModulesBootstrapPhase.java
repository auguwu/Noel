package dev.floofy.noel.bot.bootstrap.phases;

import dev.floofy.noel.bot.bootstrap.BootstrapPhase;
import org.jetbrains.annotations.NotNull;

public class ConfigureModulesBootstrapPhase implements BootstrapPhase {
    @Override
    public void bootstrap() throws Exception {
    }

    @Override
    @NotNull
    public String mdcValue() {
        return "configure:modules";
    }
}
