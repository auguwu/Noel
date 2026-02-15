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

import dev.floofy.noel.pinecone.CommandContext;
import dev.floofy.noel.pinecone.Option;
import dev.floofy.noel.pinecone.Subcommand;
import dev.floofy.noel.pinecone.java.Function3;
import dev.floofy.noel.pinecone.internals.options.ParameterTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.*;

public final class Utilities {
    private Utilities() {}

    /// Initializes all the available [`@Subcommand`][dev.floofy.noel.pinecone.annotations.Subcommand]s available
    /// in a given `clazz` with a specified initializer.
    ///
    /// @param clazz The class that we should iterate over
    /// @param initializer Initializer for constructing a new [Subcommand]
    public static HashMap<String, Subcommand> collectSubcommands(
            Class<?> clazz,
            Function3<dev.floofy.noel.pinecone.annotations.Subcommand, Method, List<Option>, Subcommand> initializer
    ) {
        final Logger log = LoggerFactory.getLogger(clazz);
        final HashMap<String, Subcommand> map = new HashMap<>();
        log.trace("Iterating over class' methods for subcommands...");

        for (Method method: clazz.getDeclaredMethods()) {
            final var annot = method.getAnnotation(dev.floofy.noel.pinecone.annotations.Subcommand.class);
            if (annot == null) {
                continue;
            }

            log.trace("Found method {} with @Subcommand annotation in class `{}'", method.getName(), clazz);
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

            final ArrayList<Option> options = new ArrayList<>();
            for (int i = 1; i < fields.length; i++) {
                final Parameter param = fields[i];
                if (!param.isAnnotationPresent(dev.floofy.noel.pinecone.annotations.Option.class)) {
                    throw new IllegalArgumentException("Expected `@Option()` annotation to be present");
                }

                final dev.floofy.noel.pinecone.annotations.Option info = param.getAnnotation(dev.floofy.noel.pinecone.annotations.Option.class);
                final Class<?> paramType = param.getType();
                final boolean isOptionalType = Optional.class.isAssignableFrom(paramType);

                Class<?> resolvedType = paramType;
                if (isOptionalType) {
                    resolvedType = (Class<?>) ((ParameterizedType) param.getParameterizedType()).getActualTypeArguments()[0];
                }

                options.add(new Option(
                    info,
                    resolvedType,
                    new ParameterTarget(i, param),
                    isOptionalType
                ));
            }

            final var name = annot.name().isEmpty()
                    ? method.getName()
                    : annot.name();

            map.put(name, initializer.invoke(annot, method, Collections.unmodifiableList(options)));
        }

        return map;
    }
}
