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

package dev.floofy.noel.pinecone.internals;

import dev.floofy.noel.pinecone.*;
import dev.floofy.noel.pinecone.annotations.Autocomplete;
import dev.floofy.noel.pinecone.internals.java.Function3;
import dev.floofy.noel.pinecone.internals.options.ParameterTarget;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Collectors {
    private static final Set<Class<?>> AUTOCOMPLETER_VALUE_TYPES =
            Set.of(String.class, OptionalLong.class, OptionalDouble.class);

    private Collectors() {}

    public static Map<String, Subcommand> subcommands(
            Class<?> cls,
            Function3<Subcommand, dev.floofy.noel.pinecone.annotations.Subcommand, Method, List<Option>> init) {
        final Logger LOG = LoggerFactory.getLogger(cls);
        final HashMap<String, Subcommand> map = new HashMap<>();

        LOG.trace("iterating over methods for subcommands [class={}]", cls.getName());
        for (var method : cls.getDeclaredMethods()) {
            final var anno = method.getAnnotation(dev.floofy.noel.pinecone.annotations.Subcommand.class);
            if (anno == null) {
                continue;
            }

            LOG.trace("found method [{}] with `@Subcommand' annotation", method.getName());

            final var fields = method.getParameters();
            if (fields.length == 0) {
                LOG.warn("method `{}' is not a viable subcommand: missing parameters", method.getName());
                continue;
            }

            if (!CommandContext.class.isAssignableFrom(fields[0].getType())) {
                LOG.warn(
                        "method `{}' is not a viable subcommand: first argument was not of class [{}] but was class"
                                + " [{}] instead",
                        method.getName(),
                        CommandContext.class.getName(),
                        fields[0].getType());

                continue;
            }

            final ArrayList<Option> options = new ArrayList<>();
            for (int i = 1; i < fields.length; i++) {
                final var param = fields[i];
                if (!param.isAnnotationPresent(dev.floofy.noel.pinecone.annotations.Option.class)) {
                    throw new IllegalArgumentException("Expected `@Option` annotation to be present");
                }

                final var info = param.getAnnotation(dev.floofy.noel.pinecone.annotations.Option.class);
                final var paramType = param.getType();
                final boolean isOptionalType = Optional.class.isAssignableFrom(paramType);

                Class<?> resolvedType = paramType;
                if (isOptionalType) {
                    resolvedType =
                            (Class<?>) ((ParameterizedType) param.getParameterizedType()).getActualTypeArguments()[0];
                }

                options.add(new Option(info, resolvedType, new ParameterTarget(i, param), isOptionalType));
            }

            final var name = anno.name().isEmpty() || anno.name().isBlank() ? method.getName() : anno.name();

            map.put(name, init.invoke(anno, method, Collections.unmodifiableList(options)));
        }

        return map;
    }

    public static Map<String, Autocompleter> autocompleters(Class<?> cls, Object instance) {
        final Logger LOG = LoggerFactory.getLogger(cls);
        final var map = new HashMap<String, Autocompleter>();

        LOG.trace("iterating over methods for autocompleters [class={}]", cls.getName());
        for (var method : cls.getDeclaredMethods()) {
            final var annot = method.getAnnotation(Autocomplete.class);
            if (annot == null) {
                continue;
            }

            final var params = method.getParameterTypes();
            final var valid = params.length == 2
                    && params[0].isAssignableFrom(AutocompleteContext.class)
                    && AUTOCOMPLETER_VALUE_TYPES.contains(params[1])
                    && Collection.class.isAssignableFrom(method.getReturnType());

            if (!valid) {
                throw new IllegalStateException(
                        "@Autocomplete method %s.%s must be (AutocompleteContext, String|OptionalDouble|OptionalLong) -> Collection<Command.Choice>"
                                .formatted(cls.getName(), method.getName()));
            }

            method.setAccessible(true);
            if (map.putIfAbsent(annot.value(), new Autocompleter(instance, method)) != null) {
                throw new IllegalStateException("option `%s' in %s has more than one @Autocomplete method"
                        .formatted(annot.value(), cls.getName()));
            }
        }

        return map;
    }
}
