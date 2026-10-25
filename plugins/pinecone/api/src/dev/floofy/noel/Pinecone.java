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

package dev.floofy.noel;

import dev.floofy.noel.pinecone.CommandInfo;
import dev.floofy.noel.pinecone.CommandInterceptor;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/// The main framework that does all the processing of slash commands.
public interface Pinecone {
    /// Returns information about every registered slash command.
    Set<CommandInfo> getSlashCommands();

    /// Adds an interceptor that runs around every slash command invocation.
    /// @param interceptor the interceptor; see [CommandInterceptor] for ordering
    /// @return a handle that removes the interceptor when closed
    AutoCloseable addInterceptor(CommandInterceptor interceptor);

    /// Whether `user` can see and run a command: Discord's default permissions
    /// allow it, and every interceptor that applies (global, and the command's own
    /// from `@Intercept`) would let them through.
    ///
    /// @param command a command from [#getSlashCommands()]
    /// @param user    the user asking
    /// @param member  the user as a guild member, or `null` outside a guild
    /// @return whether to show the command to this user
    boolean canUse(CommandInfo command, User user, Member member);

    /// Pushes every registered slash command to Discord again, replacing what is
    /// there. Does nothing before the initial sync.
    ///
    /// @return completes when Discord has accepted every command list
    CompletableFuture<Void> resync();
}
