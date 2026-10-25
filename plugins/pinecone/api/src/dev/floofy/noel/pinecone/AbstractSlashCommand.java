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

import com.google.inject.Inject;
import com.google.inject.Injector;
import dev.floofy.noel.pinecone.annotations.SlashCommand;
import dev.floofy.noel.pinecone.internals.Collectors;
import dev.floofy.noel.pinecone.internals.options.FieldTarget;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/// Base class for every slash command.
///
/// Subclasses are annotated with [SlashCommand] and live in a plugin's package; see [SlashCommand] for discovery, and
/// for the
/// three shapes a command can take: plain, with [subcommands][dev.floofy.noel.pinecone.annotations.Subcommand],
/// or with [subcommand groups][dev.floofy.noel.pinecone.annotations.SubcommandGroup].
///
/// ```java
/// @SlashCommand(name = "echo", description = "Repeats what you say")
/// public final class Echo extends AbstractSlashCommand {
///     @Option(name = "text", description = "What to say", required = true)
///     private String text;
///
///     @Override
///     public void execute(CommandContext context) {
///         context.reply(text).queue();
///     }
/// }
/// ```
///
/// ## Instances
/// Pinecone creates a **new instance for every invocation**, through the owning plugin's injector, then fills its
/// `@Option` fields
/// and calls [#execute] or the subcommand. So `@Option` fields are only ever seen by one invocation, and constructor
/// injection works.
/// One more instance per command, the *prototype*, is kept for metadata (names, options, `/help`) and for
/// [`@Autocomplete`][dev.floofy.noel.pinecone.annotations.Autocomplete]
/// handlers. It is never executed.
///
/// ## Metadata
/// The metadata getters ([#getOptions()], [#getSubcommands()], [#getSubcommandGroups()], [#getAutocompleters()]) read
/// the annotations
/// reflectively on first use and cache the result. They are thread-safe. Only members declared on the command class
/// itself are read:
/// options, subcommands and handlers inherited from a superclass are ignored.
public abstract class AbstractSlashCommand {
    private Map<String, Autocompleter> autocompleters;
    private List<SubcommandGroup> subcommandGroups;
    private Map<String, Subcommand> subcommands;
    private List<Option> options;

    @Inject
    private Injector injector;

    /// Runs a plain command.
    ///
    /// Never called for a command with subcommands or subcommand groups; those
    /// run the matching `@Subcommand` method instead. Such commands can leave this
    /// empty.
    ///
    /// @param cx the invocation's context
    /// @throws Exception anything; it goes through the command interceptors and, if none handles it, Pinecone's default
    /// error reply
    public abstract void execute(CommandContext cx) throws Exception;

    /// Returns this command's [SlashCommand] annotation.
    /// @throws IllegalStateException if the class isn't annotated
    public final SlashCommand getInfo() {
        final var info = getClass().getAnnotation(SlashCommand.class);
        if (info == null) {
            throw new IllegalStateException(getClass().getName() + " is not annotated with @SlashCommand");
        }

        return info;
    }

    /// Returns the subcommand groups declared as `static` nested classes, sorted
    /// by name. Each group's instance is created by this command's injector.
    ///
    /// @return the groups; empty if there are none
    /// @throws IllegalStateException if a group class isn't `static`, or this command wasn't created by Guice
    public final synchronized List<SubcommandGroup> getSubcommandGroups() {
        if (this.subcommandGroups == null) {
            final var groups = new ArrayList<SubcommandGroup>();
            for (var nested : getClass().getDeclaredClasses()) {
                final var anno = nested.getAnnotation(dev.floofy.noel.pinecone.annotations.SubcommandGroup.class);
                if (anno == null) {
                    continue;
                }

                // This used to log a warning and skip the class, so the group
                // silently didn't exist. A missing `static` is always a mistake.
                if (!Modifier.isStatic(nested.getModifiers())) {
                    throw new IllegalStateException(
                            "@SubcommandGroup class [%s] must be static".formatted(nested.getName()));
                }

                if (this.injector == null) {
                    throw new IllegalStateException(
                            "%s was not created by Guice, so its subcommand groups can't be built"
                                    .formatted(getClass().getName()));
                }

                final Object instance = injector.getInstance(nested);
                final var groupSubcommands = Collectors.subcommands(
                        nested, (info, method, options) -> new Subcommand(info, options, method, instance));

                groups.add(new SubcommandGroup(anno, instance, groupSubcommands));
            }

            groups.sort(Comparator.comparing(grp -> grp.getInfo().name()));
            this.subcommandGroups = List.copyOf(groups);
        }

        return this.subcommandGroups;
    }

    /// Returns the subcommands declared directly on this class, by name, sorted.
    /// @return the subcommands; empty if there are none
    /// @throws IllegalStateException if two subcommands have the same name, or a
    ///         subcommand parameter isn't an `@Option`
    public final synchronized Map<String, Subcommand> getSubcommands() {
        if (this.subcommands == null) {
            this.subcommands = Collections.unmodifiableMap(new TreeMap<>(Collectors.subcommands(
                    getClass(), (info, method, options) -> new Subcommand(info, options, method, this))));
        }

        return subcommands;
    }

    /// Returns this command's [`@Autocomplete`][dev.floofy.noel.pinecone.annotations.Autocomplete]
    /// handlers, keyed by option name, from the command class and its subcommand
    /// groups.
    ///
    /// @return the handlers; empty if there are none
    /// @throws IllegalStateException if a handler has the wrong signature, or an option has more than one
    public final synchronized Map<String, Autocompleter> getAutocompleters() {
        if (this.autocompleters == null) {
            final var found = Collectors.autocompleters(getClass(), this);
            for (var group : getSubcommandGroups()) {
                found.putAll(Collectors.autocompleters(group.getInstance().getClass(), group.getInstance()));
            }

            this.autocompleters = Map.copyOf(found);
        }

        return this.autocompleters;
    }

    /// Returns the options declared as `@Option` fields on this class, in declaration order.
    ///
    /// A command with subcommands mustn't declare any; registration reject that, since Discord doesn't allow both.
    ///
    /// @return the options; empty if there are none
    /// @throws IllegalStateException if an option's Java type isn't supported
    public final synchronized List<Option> getOptions() {
        if (this.options == null) {
            final var found = new ArrayList<Option>();
            for (var field : getClass().getDeclaredFields()) {
                final var annotation = field.getAnnotation(dev.floofy.noel.pinecone.annotations.Option.class);
                if (annotation == null) {
                    continue;
                }

                final boolean isOptional = Optional.class.isAssignableFrom(field.getType());
                final Class<?> resolvedType = isOptional ? optionalElementType(field) : field.getType();
                found.add(new Option(annotation, resolvedType, new FieldTarget(field), isOptional));
            }

            options = List.copyOf(found);
        }

        return options;
    }

    private static Class<?> optionalElementType(Field field) {
        if (!(field.getGenericType() instanceof ParameterizedType parameterized)) {
            throw new IllegalStateException("Optional field is missing its type argument: " + field);
        }

        final Type element = parameterized.getActualTypeArguments()[0];
        if (element instanceof Class<?> type) {
            return type;
        }

        if (element instanceof ParameterizedType nested) {
            return (Class<?>) nested.getRawType();
        }

        throw new IllegalStateException("unsupported Optional type argument " + element + " on " + field);
    }
}
