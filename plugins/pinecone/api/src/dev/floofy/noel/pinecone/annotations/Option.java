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

package dev.floofy.noel.pinecone.annotations;

import java.lang.annotation.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.interactions.commands.OptionType;

/// Declares a slash command option, and where its value goes.
///
/// On a **field** of a [`@SlashCommand`][SlashCommand] class, the option belongs to the command itself, and Pinecone
/// sets the field
/// before `execute` runs. On a **parameter** of a [`@Subcommand`][Subcommand] method, after the leading
/// `CommandContext`, the option
/// belongs to that subcommand and is passed as the argument.
///
/// ```java
/// // a field on a plain command
/// @Option(name = "command", description = "slash command to provide information about")
/// private Optional<String> command;
///
/// // a parameter on a subcommand
/// @Subcommand(name = "set", description = "Set your birthday")
/// public void set(
///         CommandContext context,
///         @Option(name = "month", description = "Month, 1 to 12", type = OptionType.INTEGER, required = true) int
/// month,
///         @Option(name = "timezone", description = "Your timezone") Optional<String> timezone) { ... }
/// ```
///
/// ## Java types
/// The Java type must match [#type()]:
///
/// | `type`        | Java type                                     |
/// |---------------|-----------------------------------------------|
/// | `STRING`      | `String`                                      |
/// | `INTEGER`     | `int` / `Integer`, or `long` / `Long`         |
/// | `NUMBER`      | `double` / `Double`                           |
/// | `BOOLEAN`     | `boolean` / `Boolean`                         |
/// | `USER`        | `User`, or `Member` (null outside a guild)    |
/// | `ROLE`        | `Role`                                        |
/// | `MENTIONABLE` | `IMentionable`                                |
/// | `ATTACHMENT`  | `Message.Attachment`                          |
/// | `CHANNEL`     | `GuildChannelUnion`                           |
///
/// Any of these can be wrapped in `Optional`. `CHANNEL` options aren't supported
/// yet. An unsupported Java type fails when the plugin registers.
///
/// ## Missing values
/// For an option that isn't [required][#required()], use `Optional<T>`: it is `Optional.empty()` when the user leaves
/// the option out.
/// A plain type gets `null` instead, which fails for primitives, so a non-required `int` breaks the first time someone
/// omits it.
/// Required options can use primitives safely.
///
/// To offer suggestions as the user types, add an [`@Autocomplete`][Autocomplete] method for the option.
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface Option {
    /// The option's name, as shown in Discord.
    ///
    /// Discord requires 1 to 32 characters: lowercase letters, digits, `-` and
    /// `_`. It must be unique within its command or subcommand.
    String name();

    /// The description Discord shows for the option. Discord requires 1 to 100
    /// characters.
    String description();

    /// The option's Discord type. Must match the Java type; see the table above.
    OptionType type() default OptionType.STRING;

    /// Whether the user must fill in this option.
    ///
    /// Discord requires every required option to come before every optional one,
    /// so declare required fields or parameters first.
    boolean required() default false;

    /// For a `CHANNEL` option, the kinds of channel Discord lets the user pick,
    /// such as `TEXT` and `NEWS` for a channel to post in. Empty allows every
    /// kind. Ignored for other option types.
    ///
    /// Discord only enforces this in its picker, so check the channel you get
    /// anyway.
    ///
    /// @return the allowed channel types
    ChannelType[] channelTypes() default {};
}
