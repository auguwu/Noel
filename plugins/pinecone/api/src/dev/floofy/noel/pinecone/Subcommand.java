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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

/// A [`@Subcommand`][dev.floofy.noel.pinecone.annotations.Subcommand] method,
/// bound to the object it runs on: the command instance, or the subcommand
/// group's instance.
///
/// Created by [AbstractSlashCommand] while reading a command's metadata. Its
/// name is the key it is stored under in [AbstractSlashCommand#getSubcommands()]
/// or [SubcommandGroup#getSubcommands()].
public final class Subcommand {
    private final dev.floofy.noel.pinecone.annotations.Subcommand info;
    private final List<Option> options;
    private final Method method;
    private final Object instance;

    Subcommand(
            dev.floofy.noel.pinecone.annotations.Subcommand info,
            List<Option> options,
            Method method,
            Object instance) {
        this.info = Objects.requireNonNull(info, "info");
        this.options = List.copyOf(Objects.requireNonNull(options, "options"));
        this.method = Objects.requireNonNull(method, "method");
        this.instance = Objects.requireNonNull(instance, "instance");

        this.method.setAccessible(true);
    }

    /// Returns the subcommand's options, from its `@Option` parameters, in parameter order.
    public List<Option> getOptions() {
        return options;
    }

    /// Returns the subcommand's annotation.
    public dev.floofy.noel.pinecone.annotations.Subcommand getInfo() {
        return info;
    }

    /// Resolves this subcommand's options and runs it.
    ///
    /// @param context the invocation's context
    /// @throws Exception if an option can't be resolved, or whatever the subcommand method throws, unwrapped from
    /// reflection
    public void invoke(CommandContext context) throws Exception {
        Objects.requireNonNull(context, "context");

        final Object[] args = new Object[method.getParameterCount()];
        args[0] = context;

        for (var option : options) {
            try {
                option.resolveInto(context, null, args);
            } catch (Exception ex) {
                throw new IllegalStateException(
                        "failed to resolve option `%s'"
                                .formatted(option.getInfo().name()),
                        ex);
            }
        }

        try {
            method.invoke(instance, args);
        } catch (InvocationTargetException ex) {
            // Report what the subcommand threw, not the reflection wrapper.
            if (ex.getCause() instanceof Exception cause) {
                throw cause;
            }

            if (ex.getCause() instanceof Error cause) {
                throw cause;
            }

            throw ex;
        }
    }
}
