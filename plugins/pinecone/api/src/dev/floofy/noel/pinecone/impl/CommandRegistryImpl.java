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

import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import dev.floofy.noel.pinecone.AbstractSlashCommand;
import dev.floofy.noel.pinecone.CommandInterceptor;
import dev.floofy.noel.pinecone.CommandRegistry;
import dev.floofy.noel.pinecone.annotations.Intercept;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public final class CommandRegistryImpl implements CommandRegistry {
    private final Map<String, Registered> byName = new ConcurrentHashMap<>();

    @Override
    public synchronized int register(String pluginId, Injector parent, Class<?> owner) {
        var found = CommandScanner.scan(owner);
        if (found.commands().isEmpty()) {
            return 0;
        }

        var interceptorTypes = new LinkedHashSet<Class<? extends CommandInterceptor>>();
        for (var type : found.commands()) {
            interceptorTypes.addAll(interceptorTypesOf(type));
        }

        var injector = parent.createChildInjector(binder -> {
            binder.requireExplicitBindings();
            found.commands().forEach(binder::bind);
            found.groups().forEach(binder::bind);
            interceptorTypes.forEach(binder::bind);
        });

        var added = new HashMap<String, Registered>();
        for (var type : found.commands()) {
            Provider<? extends AbstractSlashCommand> provider = injector.getProvider(type);
            var prototype = provider.get();
            var info = prototype.getInfo();

            // Read and check all metadata now. Discord commands are synced in one
            // bulk request, so a single invalid command found at sync time would
            // stop every command from being registered.
            try {
                CommandValidator.validate(prototype);
            } catch (RuntimeException ex) {
                throw new IllegalStateException(
                        "invalid slash command %s in plugin [%s]: %s"
                                .formatted(type.getName(), pluginId, ex.getMessage()),
                        ex);
            }

            var existing = byName.get(info.name());
            if (existing == null) {
                existing = added.get(info.name());
            }

            if (existing != null) {
                throw new IllegalStateException("slash command /%s from plugin [%s] is already registered by [%s]"
                        .formatted(info.name(), pluginId, existing.pluginId()));
            }

            var interceptors = interceptorTypesOf(type).stream()
                    .<CommandInterceptor>map(injector::getInstance)
                    .toList();

            added.put(info.name(), new Registered(pluginId, info, prototype, provider, interceptors));
        }

        byName.putAll(added);
        return added.size();
    }

    private static List<Class<? extends CommandInterceptor>> interceptorTypesOf(Class<?> command) {
        var annotation = command.getAnnotation(Intercept.class);
        return annotation == null ? List.of() : List.of(annotation.value());
    }

    @Override
    public synchronized void unregisterAll(String pluginId) {
        byName.values().removeIf(registered -> registered.pluginId().equals(pluginId));
    }

    @Override
    public Optional<Registered> find(String name) {
        return Optional.ofNullable(byName.get(name));
    }

    @Override
    public List<Registered> all() {
        return List.copyOf(new ArrayList<>(byName.values()));
    }
}
