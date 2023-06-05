/*
 * 🐾✨ Noel: Discord bot made to manage my servers, made in Java.
 * Copyright 2021-2023 Noel <cutie@floofy.dev>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.floofy.noel.bot.bootstrap;

import dev.floofy.noel.bot.bootstrap.phases.ConfigureModulesBootstrapPhase;
import dev.floofy.noel.bot.bootstrap.phases.PreInitBootstrapPhase;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public interface BootstrapPhase {
    List<BootstrapPhase> PHASES =
            new ArrayList<>(List.of(new PreInitBootstrapPhase(), new ConfigureModulesBootstrapPhase()));

    void bootstrap() throws Exception;

    @NotNull
    String mdcValue();
}
