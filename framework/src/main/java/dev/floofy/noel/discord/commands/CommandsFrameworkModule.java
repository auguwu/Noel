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

package dev.floofy.noel.discord.commands;

import com.google.inject.Injector;
import dev.floofy.noel.discord.commands.internal.CommandHandler;
import dev.floofy.noel.modules.jda.AbstractNoelJDAModule;
import net.dv8tion.jda.api.JDA;
import org.jetbrains.annotations.NotNull;

public class CommandsFrameworkModule extends AbstractNoelJDAModule {
    @Override
    @NotNull
    public String name() {
        return "noel:commands";
    }

    @Override
    protected void configure() {
        bind(CommandHandler.class).asEagerSingleton();
    }

    @Override
    public void configure(JDA jda, Injector injector) {
        jda.addEventListener(injector.getInstance(CommandHandler.class));
    }
}
