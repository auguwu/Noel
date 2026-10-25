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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Declares a method as a subcommand.
///
/// The method goes on a [`@SlashCommand`][SlashCommand] class, giving `/command subcommand`, or on a
/// [`@SubcommandGroup`][SubcommandGroup] class,
/// giving `/command group subcommand`. It must be `public`. Its first parameter is the `CommandContext`, and every
/// other parameter is an
/// [`@Option`][Option]:
///
/// ```java
/// @Subcommand(name = "set", description = "Set your birthday")
/// public void set(
///         CommandContext context,
///         @Option(name = "month", description = "Month, 1 to 12", type = OptionType.INTEGER, required = true) int
/// month,
///         @Option(name = "day", description = "Day of the month", type = OptionType.INTEGER, required = true) int day)
/// {
///     context.reply("saved!").setEphemeral(true).queue();
/// }
/// ```
///
/// A method whose first parameter isn't a `CommandContext` is skipped with a warning, and a parameter without `@Option`
/// fails registration. The return value is ignored. Anything the method throws goes through the command interceptors
/// and, if none
/// handles it, Pinecone's default error reply.
///
/// A command with subcommands can't have options of its own, and its `execute` is never called.
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subcommand {
    /// The subcommand's name. Defaults to the method's name, which only works if
    /// that is already a valid Discord name: 1 to 32 characters, lowercase
    /// letters, digits, `-` and `_`. `setTimezone` isn't, so name it explicitly.
    String name() default "";

    /// The description Discord shows for the subcommand.
    ///
    /// Discord requires 1 to 100 characters, so leaving this empty fails
    /// registration with Discord, despite the default.
    String description() default "";
}
