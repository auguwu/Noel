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

package dev.floofy.noel.bot.bootstrap.phases;

import dev.floofy.noel.bot.bootstrap.BootstrapPhase;
import dev.floofy.noel.config.Config;
import dev.floofy.noel.config.SecureSetting;
import dev.floofy.noel.config.Setting;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class ConfigureModulesBootstrapPhase implements BootstrapPhase {
    private final Setting<SecureSetting> discordToken = Setting.secureSetting("discord.token");
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Override
    public void bootstrap() throws Exception {
        final Config config = Config.get();
        final String token = config.get(discordToken).resolve();
        if (Objects.equals(token, "")) {
            LOG.error("Missing 'discord.token' property, please fill that in.");
            System.exit(1);
        }

        final JDA jda = JDABuilder.create(token, List.of()).build();
        jda.awaitReady();
    }

    @Override
    @NotNull
    public String mdcValue() {
        return "configure:modules";
    }
}
