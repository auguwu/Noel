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
import dev.floofy.Noel;
import dev.floofy.noel.pinecone.annotations.SlashCommand;
import dev.floofy.noel.pinecone.internals.Utilities;
import dev.floofy.noel.pinecone.internals.options.FieldTarget;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.*;

/// Represents a root slash command that has no subcommands associated with it.
public abstract class AbstractSlashCommand {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSlashCommand.class);

    private ArrayList<SubcommandGroup> subcommandGroups = null;
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

    @NotNull
    public List<SubcommandGroup> getSubcommandGroups() {
        if (subcommandGroups == null) {
            subcommandGroups = new ArrayList<>();

            LOG.trace("Processing subcommand groups for class");

            final Injector injector = Noel.getInstance().getInjector();
            for (Class<?> nestedClass: getClass().getDeclaredClasses()) {
                final var annot = nestedClass.getAnnotation(dev.floofy.noel.pinecone.annotations.SubcommandGroup.class);
                if (annot == null) {
                    continue;
                }

                LOG.trace("Found @SubcommandGroup with name `{}' in class `{}'", annot.name(), nestedClass);
                if (!Modifier.isStatic(nestedClass.getModifiers())) {
                    LOG.warn("Class `{}' was not marked `static`, skipping", nestedClass);
                    continue;
                }

                Object instance = injector.getInstance(nestedClass);
                final var subcommands = Utilities.collectSubcommands(nestedClass, (info, method, options) -> new Subcommand(info, options, method, instance));
                subcommandGroups.add(new SubcommandGroup(annot, nestedClass, subcommands));
            }
        }

        return subcommandGroups;
    }

    @NotNull
    public Map<String, Subcommand> getSubcommands() {
        if (subcommands == null) {
            subcommands = Utilities.collectSubcommands(getClass(), (info, method, options) -> new Subcommand(info, options, method, this));
        }

        return Collections.unmodifiableMap(subcommands);
    }

    @NotNull
    public List<Option> getOptions() {
        if (subcommandGroups != null && !subcommandGroups.isEmpty()) {
            return Collections.emptyList();
        }

        if (options == null) {
            options = new ArrayList<>();

            LOG.trace("Processing options for class");
            for (Field field: getClass().getDeclaredFields()) {
                final var annot = field.getAnnotation(dev.floofy.noel.pinecone.annotations.Option.class);
                if (annot == null) {
                    continue;
                }

                LOG.trace("Found field `{}' with @Option annotation", field.getName());

                Type genericType = field.getGenericType();
                Class<?> rawType = field.getType();
                final boolean isOptionalType = Optional.class.isAssignableFrom(rawType);

                Class<?> resolvedType;
                if (Optional.class.isAssignableFrom(rawType)) {
                    if (genericType instanceof ParameterizedType pt) {
                        Type innerType = pt.getActualTypeArguments()[0];
                        if (innerType instanceof Class<?> cls) {
                            resolvedType = cls;
                        } else if (innerType instanceof ParameterizedType innerPt) {
                            resolvedType = (Class<?>) innerPt.getRawType();
                        } else {
                            throw new IllegalStateException("Unsupported generic type: " + innerType);
                        }
                    } else {
                        throw new IllegalStateException("Optional field missing generic type: " + field);
                    }
                } else {
                    resolvedType = rawType;
                }

                options.add(new Option(
                        annot,
                        resolvedType,
                        new FieldTarget(field),
                        isOptionalType
                ));
            }
        }

        return Collections.unmodifiableList(options);
    }
}
