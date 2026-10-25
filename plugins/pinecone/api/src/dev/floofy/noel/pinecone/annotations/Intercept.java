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

import dev.floofy.noel.pinecone.CommandInterceptor;

import java.lang.annotation.*;

/// Runs [CommandInterceptor]s around one command only, instead of every command.
///
/// Each interceptor class is constructed once, when the command registers, by
/// the same injector as the command, so it can inject the plugin's bindings. It
/// needs an `@Inject` constructor or a non-private no-argument constructor. The
/// one instance then serves every invocation, possibly on several threads at
/// once, so it must be thread-safe.
///
/// ## Example
/// ```java
/// @SlashCommand(name = "eval", description = "Runs code")
/// @Intercept(UserAllowlist.class)
/// public final class Eval extends AbstractSlashCommand { ... }
///
/// final class UserAllowlist implements CommandInterceptor {
///     private final Settings settings;
///
///     @Inject
///     UserAllowlist(Settings settings) {
///         this.settings = settings;
///     }
///
///     @Override
///     public boolean before(CommandContext context, CommandInfo command) { ... }
/// }
/// ```
///
/// Interceptors added with [dev.floofy.noel.Pinecone#addInterceptor] run
/// first, then these, in the order listed. The ordering rules in
/// [CommandInterceptor] apply to the combined chain: the first `before` to
/// return `false` stops the command, and every interceptor in the chain sees
/// `onError`.
///
/// The interceptors apply to the whole command, including every subcommand.
/// To treat subcommands differently, check
/// `context.getInteraction().getSubcommandName()` inside the interceptor.
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Intercept {
    /// The interceptors to run around this command, in order.
    Class<? extends CommandInterceptor>[] value();
}
