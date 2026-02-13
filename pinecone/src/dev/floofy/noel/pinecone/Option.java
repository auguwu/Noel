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

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Optional;

public class Option {
    private final dev.floofy.noel.pinecone.annotations.Option info;
    private final Object instance;
    private final Field field;

    Option(
        @NotNull dev.floofy.noel.pinecone.annotations.Option info,
        @NotNull Object instance,
        @NotNull Field field
    ) {
        this.info = info;
        this.field = field;
        this.instance = instance;
    }

    public void resolve(@NotNull CommandContext context) throws Exception {
        field.setAccessible(true);

        final var option = context.getInteraction().getOption(info.name());
        if (option == null) {
            if (Optional.class.isAssignableFrom(field.getType())) {
                field.set(instance, Optional.empty());
                return;
            } else {
                throw new RequiredOptionNotFoundException(info.name());
            }
        }

        Class<?> innerType = (Class<?>)((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
        if (Optional.class.isAssignableFrom(field.getType())) {
            field.set(instance, Optional.of(resolveValueFromClass(option, innerType)));
        } else {
            field.set(instance, resolveValueFromClass(option, field.getType()));
        }
    }

    Object resolveValueFromClass(OptionMapping mapping, Class<?> type) {
        Object found;
        if (type == String.class) {
            found = mapping.getAsString();
        } else if (type == long.class) {
            found = mapping.getAsLong();
        } else if (type == double.class) {
            found = mapping.getAsDouble();
        } else if (type == int.class) {
            found = mapping.getAsInt();
        } else if (type == boolean.class) {
            found = mapping.getAsBoolean();
        } else if (type == Member.class) {
            found = mapping.getAsMember();
        } else if (type == User.class) {
            found = mapping.getAsUser();
        } else if (type == IMentionable.class) {
            found = mapping.getAsMentionable();
        } else if (type == Message.Attachment.class) {
            found = mapping.getAsAttachment();
        } else if (type == Role.class) {
            found = mapping.getAsRole();
        } else {
            throw new IllegalStateException(String.format("unable to map mapping to class `%s'", type.getName()));
        }

        return type.cast(found);
    }
}
