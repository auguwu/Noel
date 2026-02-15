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

import dev.floofy.noel.pinecone.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Dispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(Dispatcher.class);

    private Dispatcher() {}

    public static void dispatch(
        AbstractSlashCommand command,
        CommandContext context
    ) {
        final String subcommandGroupName = context.getInteraction().getSubcommandGroup();
        final String subcommandName = context.getInteraction().getSubcommandName();

        if (subcommandGroupName == null && subcommandName == null) {
            for (Option option: command.getOptions()) {
                try {
                    option.resolveInto(context, command, null);
                } catch (Exception e) {
                    LOG.error("Failed to run resolve option `{}'", option.getInfo().name(), e);
                    context.reply(":pensive: **^=~=^** failed to run command, try again later").setEphemeral(true).queue();

                    return;
                }
            }

            try {
                command.execute(context);
            } catch (Exception ex) {
                LOG.error("Failed to run slash command /{}", command.getInfo().name(), ex);
                context.reply(":pensive: **^=~=^** failed to run command").setEphemeral(true).queue();
            }

            return;
        }

        if (subcommandName != null && subcommandGroupName == null) {
            final Subcommand subcmd = command.getSubcommands().get(subcommandName);
            if (subcmd == null) {
                context.replyFormat(":question: **unknown subcommand: `%s'**", subcommandName).setEphemeral(true).queue();
                return;
            }

            try {
                subcmd.invoke(context);
            } catch (Exception ex) {
                LOG.error("Failed to run slash command /{}", command.getInfo().name(), ex);
                context.reply(":pensive: **^=~=^** failed to run command").setEphemeral(true).queue();
            }

            return;
        }

        final var group = command
                .getSubcommandGroups()
                .stream()
                .filter(grp -> grp.getInfo().name().equals(subcommandGroupName))
                .findFirst();

        if (group.isEmpty()) {
            context.replyFormat(":question: **unknown subcommand group: `%s'**", subcommandGroupName).setEphemeral(true).queue();
            return;
        }

        final Subcommand subcmd = group.get().getSubcommands().get(subcommandName);
        if (subcmd == null) {
            context.replyFormat(":question: **unknown subcommand: `%s'**", subcommandName).setEphemeral(true).queue();
            return;
        }

        try {
            subcmd.invoke(context);
        } catch (Exception ex) {
            LOG.error("Failed to run slash command /{}", command.getInfo().name(), ex);
            context.reply(":pensive: **^=~=^** failed to run command").setEphemeral(true).queue();
        }
    }
}
