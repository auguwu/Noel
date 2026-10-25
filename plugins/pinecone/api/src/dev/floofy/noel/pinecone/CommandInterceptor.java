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

import java.time.Duration;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

/// Runs around every slash command invocation, for checks, cooldowns, metrics or audit logging.
///
/// Register one with [dev.floofy.noel.Pinecone#addInterceptor(CommandInterceptor)],
/// typically from a plugin's `onPluginEnable()`, and close the returned handle in
/// `onPluginDestruct()`:
///
/// ```java
/// final class OwnerOnly implements CommandInterceptor {
///     private final Set<Long> owners;
///
///     @Override
///     public boolean before(CommandContext context, CommandInfo command) {
///         if (!command.name().equals("eval") || owners.contains(context.getInteraction().getUser().getIdLong())) {
///             return true;
///         }
///
///         context.reply(":no_entry: this command is for the bot's owners").setEphemeral(true).queue();
///         return false;
///     }
/// }
/// ```
///
/// ## Order
/// Interceptors run in the order they were added, on the command execution
/// thread:
/// 1. [#before] on each interceptor. The first to return `false` stops the
///    invocation; later interceptors and the command don't run, and neither do
///    [#after] or [#onError].
/// 2. The command runs.
/// 3. On success, [#after] on each interceptor.
/// 4. If the command or any [#before] throws, [#onError] on **every**
///    interceptor, so metrics see every failure. If none returns `true`, Pinecone
///    logs the error and sends its default error reply.
///
/// An exception thrown from [#after] or [#onError] is logged and otherwise ignored.
public interface CommandInterceptor {
    /// Called before the command runs.
    ///
    /// @param cx the invocation's context
    /// @param command the command being invoked
    /// @return `true` to continue, `false` to stop the invocation. When returning
    ///         `false`, reply to the user yourself.
    /// @throws Exception treated like an exception from the command
    default boolean before(CommandContext cx, CommandInfo command) throws Exception {
        return true;
    }

    /// Called after the command completes without throwing.
    ///
    /// The duration covers the command's own code only. Work it queued
    /// asynchronously, like a JDA `queue()`, may still be in flight.
    ///
    /// @param cx the invocation's context
    /// @param command the command that ran
    /// @param took    how long the command ran
    default void after(CommandContext cx, CommandInfo command, Duration took) {}

    /// Called when the command, or a [#before], throws.
    ///
    /// Every interceptor is called, whatever the others return. Return `true` only
    /// if this interceptor replied to the user, and make sure at most one
    /// interceptor replies.
    ///
    /// @param cx the invocation's context
    /// @param command the command that failed
    /// @param error   what was thrown
    /// @return `true` to suppress Pinecone's default error reply
    default boolean onError(CommandContext cx, CommandInfo command, Throwable error) {
        return false;
    }

    /// Whether `user` would get past [#before], for hiding commands they can't run
    /// in `/help` and autocomplete. This doesn't enforce anything; [#before] still
    /// decides when the command actually runs.
    ///
    /// Must be fast and side-effect free: no replies, no I/O. It's called for every
    /// command each time someone opens `/help` or types in its autocomplete.
    ///
    /// @param user    the user asking
    /// @param member  the user as a guild member, or `null` outside a guild
    /// @param command the command in question
    /// @return `false` to hide the command from this user
    default boolean isVisibleTo(User user, Member member, CommandInfo command) {
        return true;
    }
}
