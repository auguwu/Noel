package dev.floofy.noel.bot.bootstrap;

import dev.floofy.noel.bot.bootstrap.phases.ConfigureModulesBootstrapPhase;
import dev.floofy.noel.bot.bootstrap.phases.PreInitBootstrapPhase;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public interface BootstrapPhase {
    List<BootstrapPhase> PHASES = new ArrayList<>(List.of(
        new PreInitBootstrapPhase(),
        new ConfigureModulesBootstrapPhase()
    ));

    void bootstrap() throws Exception;

    @NotNull
    String mdcValue();
}
