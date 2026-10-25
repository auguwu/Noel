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

import dev.floofy.noel.Pinecone;
import dev.floofy.noel.pinecone.impl.AutocompleteContextImpl;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.CommandAutoCompleteInteraction;

/// An [Autocomplete][dev.floofy.noel.pinecone.annotations.Autocomplete] handler method, bound to the object that
/// declares it.
///
/// Handlers are bound to a command's prototype instance (or its subcommand groups' instances), which is shared:
/// Pinecone may call the
/// same handler from several threads at once, for different users typing at the same time. Keep handlers stateless, or
/// make their
/// state thread-safe.
///
/// ## What the handler receives
/// Discord sends the text typed into the option so far, even for numeric
/// options. The handler's second parameter decides how Pinecone passes it on:
///
/// | Parameter      | Option type | Value                                                  |
/// |----------------|-------------|--------------------------------------------------------|
/// | `String`       | any         | the raw text, possibly empty                           |
/// | `OptionalLong` | `INTEGER`   | the number, or empty while the text isn't one yet      |
/// | `OptionalDouble` | `NUMBER`  | the number, or empty while the text isn't one yet      |
///
/// The numeric forms are empty when the field is blank or half-typed (`""`,
/// `"-"`), so a handler can show its default suggestions then. There are no
/// primitive forms, since a primitive can't represent "nothing typed yet".
///
/// ## What the handler returns
/// The choices' values must match the option's type: `String` values for a
/// `STRING` option, `long` values for `INTEGER`, `double` for `NUMBER`.
/// Mismatched choices are dropped, with a warning, because Discord rejects the
/// whole reply if any choice has the wrong type.
public final class Autocompleter {
    /// Discord shows at most this many choices.
    public static final int MAX_CHOICES = 25;

    private final Class<?> valueType;
    private final Object instance;
    private final Method method;

    public Autocompleter(Object instance, Method method) {
        this.instance = instance;
        this.method = method;
        this.valueType = method.getParameterTypes()[1];
    }

    /// Returns the type of the handler's second parameter: `String`,
    /// `OptionalLong` or `OptionalDouble`.
    public Class<?> getValueType() {
        return valueType;
    }

    /// Runs the handler.
    /// @param pinecone    [Pinecone] instance where this autocompleter is from.
    /// @param interaction the autocomplete interaction
    /// @param typed       what the user has typed so far
    /// @return at most [#MAX_CHOICES] choices
    /// @throws Exception whatever the handler thrown
    public List<Command.Choice> complete(Pinecone pinecone, CommandAutoCompleteInteraction interaction, String typed)
            throws Exception {
        Objects.requireNonNull(interaction, "interaction");
        Objects.requireNonNull(typed, "typed");

        final Object value = convert(typed.strip());
        final Object result;
        try {
            result = method.invoke(
                    instance,
                    new AutocompleteContextImpl(interaction, pinecone),
                    valueType == String.class ? typed : value);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof Exception cause) {
                throw cause;
            }

            throw ex;
        }

        if (result == null) {
            return List.of();
        }

        return ((Collection<Command.Choice>) result).stream().limit(MAX_CHOICES).toList();
    }

    private Object convert(String text) {
        if (valueType == OptionalLong.class) {
            try {
                return OptionalLong.of(Long.parseLong(text));
            } catch (NumberFormatException ex) {
                return OptionalLong.empty();
            }
        }

        if (valueType == OptionalDouble.class) {
            try {
                final double number = Double.parseDouble(text);
                return Double.isFinite(number) ? OptionalDouble.of(number) : OptionalDouble.empty();
            } catch (NumberFormatException ex) {
                return OptionalDouble.empty();
            }
        }

        return text;
    }
}
