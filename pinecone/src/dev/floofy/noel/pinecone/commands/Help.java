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

package dev.floofy.noel.pinecone.commands;

import dev.floofy.noel.pinecone.AbstractSlashCommand;
import dev.floofy.noel.pinecone.CommandContext;
import dev.floofy.noel.pinecone.annotations.Option;
import dev.floofy.noel.pinecone.annotations.SlashCommand;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@SlashCommand(
        name = "help",
        description = "Displays a list of all the commands available or information about a single slash command"
)
public class Help extends AbstractSlashCommand {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Option(name = "command", description = "slash command to provide information about")
    private Optional<String> command;

    @Override
    public void execute(CommandContext context) throws Exception {
        if (command.isEmpty()) {
            final var display = getHelpDisplayText(context);
            final var message = new MessageCreateBuilder()
                    .useComponentsV2()
                    .addComponents(List.of(display))
                    .build();

            context.getInteraction().reply(message).queue();
            return;
        }

        final var commands = context.getPinecone().getSlashCommands();
        final Optional<AbstractSlashCommand> slashCommandOpt = commands
                .stream()
                .filter(cmd -> cmd.getInfo().name().equals(command.get()))
                .findAny();

        if (slashCommandOpt.isEmpty()) {
            context
                    .getInteraction()
                    .reply(String.format(":question: **~** unknown command: `%s`", command.get()))
                    .setEphemeral(true)
                    .queue();

            return;
        }

        final AbstractSlashCommand slashCommand = slashCommandOpt.get();
        final var display = getHelpDisplayTextForCommand(context, slashCommand);
        final var message = new MessageCreateBuilder()
                .useComponentsV2()
                .addComponents(List.of(display))
                .build();

        context.getInteraction().reply(message).queue();
    }

    @SuppressWarnings("DuplicatedCode")
    TextDisplay getHelpDisplayText(CommandContext context) {
        final var guild = context.getInteraction().getGuild();
        final var self = context.getInteraction().getJDA().getSelfUser();
        assert guild != null : "guild should exist";

        final var commands = context.getPinecone().getSlashCommands();
        final var globalCommands = commands.stream()
                .filter(cmd -> cmd.getInfo().onlyInGuilds().length == 0)
                .toList();

        final var guildOnlyCommands = commands.stream()
                .filter(cmd -> Arrays.stream(cmd.getInfo().onlyInGuilds()).anyMatch(id -> id == guild.getIdLong()))
                .toList();

        final StringBuilder builder = new StringBuilder();
        builder.append(String.format("## %s\n", self.getName()));
        builder.append("### Global Commands\n");

        final var maxGlobalCommandLength = calculateMaxStringLength(globalCommands, cmd -> cmd.getInfo().name());
        for (AbstractSlashCommand global: globalCommands) {
            builder.append("* `/");
            builder.append(global.getInfo().name());
            builder.append('`');
            builder.repeat(' ', maxGlobalCommandLength);
            builder.append(global.getInfo().description());
            builder.append('\n');
        }

        if (!guildOnlyCommands.isEmpty()) {
            builder.append('\n');
            builder.append(String.format("## Commands for %s\n", guild.getName()));

            final var maxGuildOnlyCommandSize = calculateMaxStringLength(guildOnlyCommands, cmd -> cmd.getInfo().name());
            for (AbstractSlashCommand cmd: guildOnlyCommands) {
                builder.append("* `/");
                builder.append(cmd.getInfo().name());
                builder.append('`');
                builder.repeat(' ', maxGuildOnlyCommandSize);
                builder.append(cmd.getInfo().description());
                builder.append('\n');
            }
        }

        return TextDisplay.of(builder.toString());
    }

    TextDisplay getHelpDisplayTextForCommand(CommandContext context, AbstractSlashCommand slashCommand) {
        final StringBuilder builder = new StringBuilder();
        builder.append("## Slash Command `/");
        builder.append(slashCommand.getInfo().name());
        builder.append("`\n> *");
        builder.append(slashCommand.getInfo().description());
        builder.append("*\n\n");

        final var subcommands = slashCommand.getSubcommands();
        if (!subcommands.isEmpty()) {
            builder.append("### Subcommands");
        }

        return TextDisplay.of(builder.toString());
    }

    /*
        final var msg = new StringBuilder();
        msg.append("## slash command `/");
        msg.append(cmd2.getInfo().name());
        msg.append("`\n");
        msg.append("> ");
        msg.append(cmd2.getInfo().description());

        var first = true;
        final var maxOptLength = cmd2.getOptions().stream()
                .map(opt -> opt.getInfo().name())
                .mapToInt(String::length)
                .max()
                .orElse(0);

        for (var opt: cmd2.getOptions()) {
            if (first) {
                msg.append('\n');
                msg.append("## Options\n");

                first = false;
            }

            msg.append("* `");
            msg.append(opt.getInfo().name());
            msg.append('`');
            msg.append(" (");
            msg.append(opt.getInfo().type().name().toLowerCase(Locale.ROOT));
            msg.append(")");
            msg.repeat(' ', maxOptLength);
            msg.append(opt.getInfo().description());
            msg.append('\n');
        }

        ctx.getInteraction().reply(new MessageCreateBuilder().useComponentsV2().addComponents(List.of(TextDisplay.of(msg.toString()))).build()).queue();
    }
 */


    <T> int calculateMaxStringLength(List<T> list, Function<T, String> mapper) {
        return list.stream()
                .map(mapper)
                .mapToInt(String::length)
                .max()
                .orElse(0);
    }
}
