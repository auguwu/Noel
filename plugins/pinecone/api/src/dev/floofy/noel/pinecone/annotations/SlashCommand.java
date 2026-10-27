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

import dev.floofy.noel.pinecone.api.AbstractSlashCommand;
import java.lang.annotation.*;
import net.dv8tion.jda.api.Permission;

/// Declares a class as a slash command.
///
/// The class must be a concrete subclass of [AbstractSlashCommand][AbstractSlashCommand] in a
/// plugin's package (or a subpackage), and the plugin must list `PineconePlugin` in its `dependsOn`. Pinecone then
/// finds it
/// when the plugin enables, constructs it with the plugin's injector, and registers it with Discord. No other
/// registration
/// is needed.
///
/// ## Example
/// ```java
/// @SlashCommand(name = "ping", description = "Checks that Noel is alive")
/// public final class Ping extends AbstractSlashCommand {
///     @Override
///     public void execute(CommandContext cx) {
///         cx.reply(":ping_pong: **Pong!**").queue();
///     }
/// }
/// ```
///
/// ## Shapes
/// A command is one of:
/// - **A plain command**, with [`@Option`][Option] fields and its logic in `execute`.
/// - **A command with subcommands**, with [`@Subcommand`][Subcommand] methods
///   (`/birthday set`, `/birthday clear`). Discord doesn't allow a command to have
///   both options and subcommands, so `execute` is never called and options belong on the subcommand methods.
/// - **A command with subcommand groups**, with `static` nested [`@SubcommandGroup`][SubcommandGroup] classes (`/config
/// roles add`).
///
/// ## Instances
/// Every invocation runs on a **new instance** of the class, built by the plugin's injector. Constructor injection is
/// the norm,
/// and `@Option` fields are safe because no two invocations share an instance. Keep constructors cheap, since one runs
/// per
/// invocation, and keep state that must outlive an invocation in an injected service.
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SlashCommand {
    /// The command's name, as typed after the `/`.
    ///
    /// Discord requires 1 to 32 characters: lowercase letters, digits, `-` and
    /// `_`. It must be unique across every plugin; a second command with the
    /// same name fails its plugin's registration, naming both plugins.
    String name();

    /// The description Discord shows in the command picker, also used by `/help`.
    ///
    /// Discord requires 1 to 100 characters, so leaving this empty fails
    /// registration with Discord, despite the default.
    String description() default "";

    /// The guilds this command is registered in. Empty, the default, registers it
    /// globally.
    ///
    /// The configuration overrides this: when `commands.<name>.onlyInGuilds` is
    /// set, that list is used instead, and an empty list there makes the command
    /// global. This makes it possible to move a command without a rebuild:
    ///
    /// ```yaml
    /// commands:
    ///   eval:
    ///     onlyInGuilds: ["824066105102303232"]
    /// ```
    ///
    /// Guild commands appear immediately; global ones can take a while to show up
    /// in every client.
    long[] onlyInGuilds() default {};

    Permission[] defaultPermissions() default {};
}
