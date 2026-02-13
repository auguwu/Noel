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

package dev.floofy.noel.pinecone.impl;

import dev.floofy.noel.Pinecone;
import dev.floofy.noel.pinecone.AbstractSlashCommand;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PineconeImpl extends ListenerAdapter implements Pinecone {
    private static final Logger LOG = LoggerFactory.getLogger(PineconeImpl.class);

    private final ExecutorService executor = Executors.newCachedThreadPool((runnable) -> new Thread(runnable, "Noel-CommandExecution"));
    private final AtomicBoolean registered = new AtomicBoolean();

    @Override
    public Set<AbstractSlashCommand> getSlashCommands() {
        return Set.of();
    }
}
