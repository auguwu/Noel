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
