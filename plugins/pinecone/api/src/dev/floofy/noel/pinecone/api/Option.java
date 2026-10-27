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
///
package dev.floofy.noel.pinecone.api;

import dev.floofy.noel.pinecone.internals.options.FieldTarget;
import dev.floofy.noel.pinecone.internals.options.ParameterTarget;
import dev.floofy.noel.pinecone.internals.options.Resolver;
import dev.floofy.noel.pinecone.internals.options.Target;
import java.util.Objects;
import java.util.Optional;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Option {
    private static final Logger log = LoggerFactory.getLogger(Option.class);
    private final dev.floofy.noel.pinecone.annotations.Option info;
    private final Class<?> resolvedClass;
    private final boolean isOptionalType;
    private final Resolver resolver;
    private final Target target;

    public Option(
            dev.floofy.noel.pinecone.annotations.Option info,
            Class<?> resolvedClass,
            Target target,
            boolean isOptionalType) {
        this.info = Objects.requireNonNull(info, "info");
        this.resolvedClass = Objects.requireNonNull(resolvedClass, "resolvedClass");
        this.isOptionalType = isOptionalType;
        this.resolver = createResolverForClass(resolvedClass);
        this.target = Objects.requireNonNull(target, "target");
    }

    public static Resolver createResolverForClass(Class<?> type) {
        if (type == String.class) {
            return OptionMapping::getAsString;
        } else if (type == long.class || type == Long.class) {
            return OptionMapping::getAsLong;
        } else if (type == double.class || type == Double.class) {
            return OptionMapping::getAsDouble;
        } else if (type == int.class || type == Integer.class) {
            return OptionMapping::getAsInt;
        } else if (type == boolean.class || type == Boolean.class) {
            return OptionMapping::getAsBoolean;
        } else if (type == Member.class) {
            return OptionMapping::getAsMember;
        } else if (type == User.class) {
            return OptionMapping::getAsUser;
        } else if (type == IMentionable.class) {
            return OptionMapping::getAsMentionable;
        } else if (type == Message.Attachment.class) {
            return OptionMapping::getAsAttachment;
        } else if (type == Role.class) {
            return OptionMapping::getAsRole;
        } else if (type == GuildChannelUnion.class) {
            return OptionMapping::getAsChannel;
        }

        throw new IllegalStateException(String.format("unable to map mapping to class `%s'", type.getName()));
    }

    public dev.floofy.noel.pinecone.annotations.Option getInfo() {
        return info;
    }

    /// Returns the Java type the option's value is converted to, with any `Optional` unwrapped.
    public Class<?> getResolvedClass() {
        return resolvedClass;
    }

    /// Returns whether the option is declared as an `Optional`.
    public boolean isOptionalType() {
        return isOptionalType;
    }

    public void resolveInto(CommandContext context, Object instance, Object[] args) throws Exception {
        Objects.requireNonNull(context, "context");

        final OptionMapping mapping = context.getInteraction().getOption(info.name());
        Object value = null;

        if (mapping != null) {
            final Object resolved = resolver.resolve(mapping);
            value = isOptionalType ? Optional.of(resolved) : resolved;
        } else if (isOptionalType) {
            value = Optional.empty();
        }

        if (target instanceof ParameterTarget param) {
            assert args != null : "for `ParameterTarget', expected `args' to not be null";
            param.inject(args, value);
        } else if (target instanceof FieldTarget field) {
            assert instance != null : "for `FieldTarget', expected `instance' to not be null";
            field.inject(instance, value);
        }
    }
}
