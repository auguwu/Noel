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

import dev.floofy.noel.pinecone.annotations.SlashCommand;
import dev.floofy.noel.pinecone.annotations.SubcommandGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/// Represents a root slash command that has no subcommands associated with it.
public abstract class AbstractSlashCommand {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private HashMap<String, Subcommand> subcommands = null;
    private ArrayList<Option> options = null;
    private SlashCommand info = null;

    /// Executes the slash command.
    /// @param context command context
    /// @throws Exception exceptions are propagated in the framework and will be reported
    public abstract void execute(CommandContext context) throws Exception;

    /// Returns the information about this slash command via the [SlashCommand] annotation.
    public SlashCommand getInfo() {
        if (info == null) {
            final var annot = getClass().getAnnotation(SlashCommand.class);
            if (annot == null) {
                throw new IllegalStateException("@SlashCommand annotation was not provided");
            }

            info = annot;
        }

        return info;
    }

    public boolean isSubcommandGroup() {
        return getClass().isAnnotationPresent(SubcommandGroup.class);
    }

    public Map<String, Subcommand> getSubcommands() {
        if (subcommands == null) {
            subcommands = new HashMap<>();

            log.trace("Processing subcommands for class");
            for (Method method: getClass().getDeclaredMethods()) {
                final var annot = method.getAnnotation(dev.floofy.noel.pinecone.annotations.Subcommand.class);
                if (annot == null) {
                    continue;
                }

                log.trace("Found method {} with @Subcommand annotation", method.getName());
                final var fields = method.getParameters();
                if (fields.length == 0) {
                    log.warn("Method {} is not a viable subcommand: has no parameters", method.getName());
                    continue;
                }

                if (!CommandContext.class.isAssignableFrom(fields[0].getType())) {
                    log.warn("Method {} is not a viable subcommand: first argument was not of class {} but {} instead",
                            method.getName(),
                            CommandContext.class.getName(),
                            fields[0].getType());

                    continue;
                }

                boolean valid = true;
                for (var param: fields) {
                    if (!param.isAnnotationPresent(dev.floofy.noel.pinecone.annotations.Option.class)) {
                        log.warn("Method {} is not a viable subcommand: argument by name `{}' doesn't have `@Option` available",
                                method.getName(),
                                param.getName());

                        valid = false;
                    }
                }

                if (valid) {
                    final var name = annot.name().isEmpty()
                            ? method.getName()
                            : annot.name();

                    subcommands.put(name, new Subcommand(annot, options, method, this));
                }
            }
        }

        return Collections.unmodifiableMap(subcommands);
    }

    public List<Option> getOptions() {
        if (isSubcommandGroup()) {
            return Collections.emptyList();
        }

        if (options == null) {
            options = new ArrayList<>();

            log.trace("Processing options for class");
            for (Field field: getClass().getDeclaredFields()) {
                final var annot = field.getAnnotation(dev.floofy.noel.pinecone.annotations.Option.class);
                if (annot == null) {
                    continue;
                }

                log.trace("Found field `{}' with @Option annotation", field.getName());
                options.add(new Option(annot, this, field));
            }
        }

        return Collections.unmodifiableList(options);
    }
}
