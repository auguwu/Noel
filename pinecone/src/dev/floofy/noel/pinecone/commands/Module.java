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

package dev.floofy.noel.pinecone.commands;

import com.google.inject.multibindings.Multibinder;

import dev.floofy.noel.modules.AbstractNoelModule;
import dev.floofy.noel.pinecone.AbstractSlashCommand;

@dev.floofy.noel.modules.annotations.Module(
        name = "commands:general",
        description = "General commands that the framework provides",
        priority = 100)
public final class Module extends AbstractNoelModule {
    @Override
    protected void configure() {
        final Multibinder<AbstractSlashCommand> binder =
                Multibinder.newSetBinder(binder(), AbstractSlashCommand.class);

        binder.addBinding().to(Help.class);
        binder.addBinding().to(Ping.class);
        binder.addBinding().to(Uptime.class);
        binder.addBinding().to(AboutMe.class);
    }
}
