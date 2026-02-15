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

package dev.floofy.noel.pinecone;

import com.google.inject.Injector;
import dev.floofy.noel.Pinecone;
import dev.floofy.noel.modules.AbstractNoelModule;
import dev.floofy.noel.modules.annotations.Initializer;
import dev.floofy.noel.pinecone.impl.PineconeImpl;
import net.dv8tion.jda.api.JDA;
import org.jetbrains.annotations.NotNull;

@dev.floofy.noel.modules.annotations.Module(
        name = "pinecone",
        description = "Module that registers Pinecone, the slash command framework that powers everything",
        priority = 250
)
public class Module extends AbstractNoelModule {
    @Override
    protected void configure() {
        bind(Pinecone.class).to(PineconeImpl.class);
    }

    @Initializer
    public void initialize(@NotNull Injector injector) {
        final JDA jda = injector.getInstance(JDA.class);
        jda.addEventListener(injector.getInstance(Pinecone.class));
    }
}
