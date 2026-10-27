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

package dev.floofy.noel.pinecone.impl;

import dev.floofy.noel.pinecone.api.AbstractSlashCommand;
import dev.floofy.noel.pinecone.api.CommandContext;
import dev.floofy.noel.pinecone.api.Subcommand;

/// Routes one invocation to the right code: a plain command's `execute`, a subcommand, or a subcommand inside a group.
/// Called by [PineconeImpl] on a command execution thread, after the command interceptors' `before` hooks have passed.
///
/// ## What it answers itself
/// Failures propagate to the caller, which runs them through the interceptors
/// and sends the default error reply. The dispatcher only replies when the
/// request itself doesn't match the command, because that's a mismatch between
/// Discord and Noel rather than a bug in the command:
///
/// - **An unknown subcommand or group.** The user's client still has an older
///   definition of the command, from before a subcommand was removed or renamed.
/// - **No subcommand, for a command that has subcommands.** The client has a
///   definition from before the command gained them. Calling `execute` here
///   would do nothing, and the user would see "The application did not
///   respond".
///
/// Discord clients refresh their command definitions on their own, usually
/// within a minute of a sync, so both cases are short-lived.
public final class Dispatcher {
    /// The ephemeral reply sent when an invocation doesn't match the command's
    /// current shape, because the user's client is still using an older
    /// definition of the command from before the last sync.
    ///
    /// It covers three cases: no subcommand for a command that now has
    /// subcommands, an unknown subcommand, and an unknown subcommand group. Each
    /// means the client needs to fetch the new definition, which reopening the
    /// command from the command list does.
    private static final String OUTDATED =
            ":hourglass: **|> this command was just updated; reopen it from the command list and try again?**";

    private Dispatcher() {}

    /// Runs `command` for the interaction in `context`.
    /// @param command a fresh instance of the command, used for this invocation only
    /// @param context the invocation's context
    /// @throws Exception if resolving an option or running the command fails
    public static void dispatch(AbstractSlashCommand command, CommandContext context) throws Exception {
        final var groupName = context.getInteraction().getSubcommandGroup();
        final var subcommandName = context.getInteraction().getSubcommandName();

        // a regular command, like `/help`, `/help ping` (ping is an option)
        if (groupName == null && subcommandName == null) {
            // Read `OUTDATED`'s doc comment on why this is a thing
            if (!command.getSubcommands().isEmpty()
                    || !command.getSubcommandGroups().isEmpty()) {
                context.reply(OUTDATED).setEphemeral(true).queue();
                return;
            }

            for (var option : command.getOptions()) {
                try {
                    option.resolveInto(context, command, null);
                } catch (Exception ex) {
                    throw new IllegalStateException(
                            "failed to resolve option `%s'"
                                    .formatted(option.getInfo().name()),
                            ex);
                }
            }

            command.execute(context);
            return;
        }

        final Subcommand subcmd;
        if (groupName == null) {
            subcmd = command.getSubcommands().get(subcommandName);
        } else {
            subcmd = command.getSubcommandGroups().stream()
                    .filter(grp -> grp.getInfo().name().equals(groupName))
                    .findFirst()
                    .map(grp -> grp.getSubcommands().get(subcommandName))
                    .orElse(null);
        }

        if (subcmd == null) {
            context.reply(OUTDATED).setEphemeral(true).queue();
            return;
        }

        subcmd.invoke(context);
    }
}
