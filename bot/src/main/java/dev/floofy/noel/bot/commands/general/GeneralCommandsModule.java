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

package dev.floofy.noel.bot.commands.general;

import com.google.inject.multibindings.Multibinder;
import dev.floofy.noel.discord.commands.AbstractCommand;
import dev.floofy.noel.modules.AbstractNoelModule;
import dev.floofy.noel.modules.annotations.ModulePriority;
import org.jetbrains.annotations.NotNull;

@ModulePriority(100)
public class GeneralCommandsModule extends AbstractNoelModule {
    @Override
    protected void configure() {
        final Multibinder<AbstractCommand> commandMultibinder =
                Multibinder.newSetBinder(binder(), AbstractCommand.class);

        commandMultibinder.addBinding().to(HelpCommand.class);
    }

    @Override
    @NotNull
    public String name() {
        return "noel:commands:general";
    }
}
