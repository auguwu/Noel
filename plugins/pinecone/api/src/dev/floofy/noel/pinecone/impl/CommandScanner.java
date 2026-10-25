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

import dev.floofy.noel.pinecone.AbstractSlashCommand;
import dev.floofy.noel.pinecone.annotations.SlashCommand;
import dev.floofy.noel.pinecone.annotations.SubcommandGroup;
import io.github.classgraph.ClassGraph;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/// Finds the slash commands a plugin ships.
///
/// Scans the plugin's package (and subpackages) through the plugin's own class
/// loader for concrete [AbstractSlashCommand] subclasses annotated with
/// [SlashCommand], plus the `static` nested classes of those commands annotated
/// with [SubcommandGroup]. Both need explicit bindings, because plugin injectors
/// require them.
///
/// Each plugin must have its own package root, or it will pick up another
/// plugin's commands.
final class CommandScanner {
    /// The classes found for one plugin.
    /// @param commands the slash command classes
    /// @param groups   the subcommand group classes nested in them
    record Found(List<Class<? extends AbstractSlashCommand>> commands, List<Class<?>> groups) {}

    /// Scans for the commands belonging to the plugin `owner`.
    /// @param owner the plugin class; its loader and package bound the scan
    /// @return the commands and subcommand groups found
    static Found scan(Class<?> owner) {
        var loader = owner.getClassLoader();
        List<String> names;
        try (var scan = new ClassGraph()
                .overrideClassLoaders(loader)
                .acceptPackages(owner.getPackageName())
                .enableClassInfo()
                .enableAnnotationInfo()
                .enableExternalClasses()
                .scan()) {
            names = scan.getSubclasses(AbstractSlashCommand.class)
                    .filter(info -> !info.isAbstract() && info.hasAnnotation(SlashCommand.class))
                    .getNames();
        }

        var commands = new ArrayList<Class<? extends AbstractSlashCommand>>();
        var groups = new ArrayList<Class<?>>();
        for (var name : names) {
            Class<? extends AbstractSlashCommand> command;
            try {
                command = Class.forName(name, false, loader).asSubclass(AbstractSlashCommand.class);
            } catch (ClassNotFoundException | LinkageError ex) {
                throw new IllegalStateException("failed to load slash command class [%s]".formatted(name), ex);
            }

            if (command.getClassLoader() != loader) {
                throw new IllegalStateException("slash command [%s] was loaded by %s, expected the loader of %s"
                        .formatted(name, command.getClassLoader(), owner.getName()));
            }

            commands.add(command);
            for (var nested : command.getDeclaredClasses()) {
                if (nested.isAnnotationPresent(SubcommandGroup.class) && Modifier.isStatic(nested.getModifiers())) {
                    groups.add(nested);
                }
            }
        }

        return new Found(List.copyOf(commands), List.copyOf(groups));
    }
}
