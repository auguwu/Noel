/// 🐾✨ Noel: Discord bot made to manage my servers made in Java
/// Copyright 2021-2026 Noel Towa <cutie@floofy.dev>
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.

package dev.floofy.noel;

import dev.floofy.Noel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

public final class Bootstrap {
    private static final Logger LOG = LoggerFactory.getLogger(Bootstrap.class);

    public static void main(String[] args) {
        Thread.currentThread().setName("Noel-Bootstrap");
        Runtime.getRuntime().addShutdownHook(new Thread(Noel::shutdown, "Noel-ShutdownThread"));

        LOG.info(
                "Bootstrapping Noel v{}+{} (built at {})",
                BuildInfo.getVersion(),
                BuildInfo.getGitCommit(),
                new SimpleDateFormat("MMMM dd, yyyy @ hh:mm:ss z")
                        .format(Date.from(Instant.parse(BuildInfo.getBuildTimestamp()))));

        final Noel noel = new Noel();
        try {
            noel.bootstrap();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
