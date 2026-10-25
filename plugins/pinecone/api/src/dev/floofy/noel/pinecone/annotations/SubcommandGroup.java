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

/// Declares a `static` nested class of a [`@SlashCommand`][SlashCommand] as a subcommand group. Its
/// [`@Subcommand`][Subcommand] methods
/// become `/command group subcommand`:
///
/// ```java
/// @SlashCommand(name = "config", description = "Server settings")
/// public final class Config extends AbstractSlashCommand {
///     @Override
///     public void execute(CommandContext context) {}
///
///     @SubcommandGroup(name = "roles", description = "Self-assignable roles")
///     public static final class Roles {
///         private final RoleStore roles;
///
///         @Inject
///         Roles(RoleStore roles) {
///             this.roles = roles;
///         }
///
///         @Subcommand(name = "add", description = "Make a role self-assignable")
///         public void add(
///                 CommandContext context,
///                 @Option(name = "role", description = "The role", type = OptionType.ROLE, required = true) Role role)
/// {
///             ...
///         }
///     }
/// }
/// ```
///
/// The class must be `static`; a non-static nested class is skipped with a warning. Pinecone constructs it with the
/// same injector as
/// the command, so it can inject the plugin's bindings. Like the command, it is created once per invocation.
///
/// Discord allows one level of grouping: a group contains subcommands, not other groups.
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SubcommandGroup {
    /// The group's name, as typed between the command and the subcommand.
    ///
    /// Discord requires 1 to 32 characters: lowercase letters, digits, `-` and
    /// `_`.
    String name();

    /// The description Discord shows for the group. Discord requires 1 to 100 characters.
    String description() default "No description available";
}
