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

import dev.floofy.Noel;
import dev.floofy.noel.pinecone.*;
import dev.floofy.noel.pinecone.annotations.Autocomplete;
import dev.floofy.noel.pinecone.annotations.Option;
import dev.floofy.noel.pinecone.annotations.SlashCommand;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.*;

@SlashCommand(name = "help", description = "View a list of available slash commands or help for a specific command")
public final class Help extends AbstractSlashCommand {
    /// Discord's limit on a choice's display name.
    private static final int MAX_CHOICE_NAME = 100;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Option(name = "command", description = "slash command to provide information about")
    private Optional<String> command;

    @Override
    public void execute(CommandContext cx) throws Exception {
        if (command.isEmpty()) {
            final var display = getHelpDisplayText(cx);
            if (display == null) {
                return;
            }

            cx.reply(display).queue();
            return;
        }

        final var guild = cx.getGuild();
        final var name = command.get().strip().replaceFirst("^/", "").toLowerCase(Locale.ROOT);
        final var cmd = cx.getPinecone()
                .getSlashCommands()
                .stream()
                .filter(c -> c.name().equals(name))
                .filter(c -> guild == null ? c.isGlobal() : c.isAvailableIn(guild.getIdLong()))
                .findFirst();

        if (cmd.isEmpty()) {
            cx.replyFormat(":question: **|> command /%s is not available or not found**", name)
                    .setEphemeral(true)
                    .queue();

            return;
        }

        final var theCommand = cmd.get();
        final var children = new ArrayList<ContainerChildComponent>();
        children.add(TextDisplay.ofFormat("## Slash Command [`/%s`]", theCommand.name()));
        children.add(TextDisplay.ofFormat("*%s*", theCommand.description()));

        if (!theCommand.subcommands().isEmpty() || !theCommand.groups().isEmpty()) {
            final var builder = new StringBuilder();
            for (var group : theCommand.groups().values()) {
                for (var sub : group.subcommands().values()) {
                    appendSubcommand(builder, "%s %s".formatted(group.name(), sub.name()), sub);
                }
            }

            for (var sub : theCommand.subcommands().values()) {
                appendSubcommand(builder, sub.name(), sub);
            }

            children.add(Separator.createDivider(Separator.Spacing.LARGE));
            children.add(TextDisplay.of("### Subcommands\n" + builder));
        }

        cx.reply(new MessageCreateBuilder()
                        .useComponentsV2()
                        .addComponents(Container.of(children).withAccentColor(Noel.EMBED_COLOUR))
                        .build())
                .setEphemeral(true)
                .queue();
    }

    MessageCreateData getHelpDisplayText(CommandContext cx) {
        final var self = cx.getSelfUser();
        final var guild = cx.getGuild();
        if (guild == null) {
            cx.reply(":question: **|> this command requires to be called in a guild.**").queue();
            return null;
        }

        final var pinecone = cx.getPinecone();
        final var commands = pinecone.getSlashCommands();
        final var globalCommands = commands
                .stream()
                .filter(cmd -> cmd.guilds().isEmpty() && pinecone.canUse(cmd, cx.getUser(), cx.getMember()))
                .map(cmd -> String.format("**`/%s`**", cmd.name()))
                .toList();

        final var thisGuildCommands = commands
                .stream()
                .filter(cmd -> pinecone.canUse(cmd, cx.getUser(), cx.getMember()))
                .filter(cmd -> !cmd.guilds().isEmpty() && cmd.guilds().contains(guild.getIdLong()))
                .map(cmd -> String.format("**`/%s`**", cmd.name()))
                .toList();

        return new MessageCreateBuilder()
                .useComponentsV2()
                .addComponents(Container.of(
                        Section.of(
                                Thumbnail.fromUrl(self.getEffectiveAvatarUrl()),
                                TextDisplay.of("## Commands"),
                                TextDisplay.of(String.join(", ", globalCommands))
                        ),
                        Separator.createDivider(Separator.Spacing.LARGE),
                        TextDisplay.of("## Guild Specific"),
                        TextDisplay.of(thisGuildCommands.isEmpty() ? "*no guild-specific commands available*" : String.join(", ", thisGuildCommands)),
                        Separator.createDivider(Separator.Spacing.SMALL),
                        TextDisplay.of("*Tip*: Run `/help [command]` to get more information about a command")
                ).withAccentColor(Noel.EMBED_COLOUR))
                .build();
    }

    @Autocomplete("command")
    public List<Command.Choice> autocompleteCommand(AutocompleteContext cx, String typed) {
        final var query = typed.strip().toLowerCase(Locale.ROOT).replaceFirst("^/", "");
        final var guild = cx.getGuild();
        final var pinecone = cx.getPinecone();

        return pinecone.getSlashCommands().stream()
                .filter(cmd -> guild == null ? cmd.isGlobal() : cmd.isAvailableIn(guild.getIdLong()))
                .filter(cmd -> cmd.name().contains(query))
                .filter(cmd -> pinecone.canUse(cmd, cx.getUser(), cx.getMember()))
                .sorted(Comparator.comparing((CommandInfo cmd) -> !cmd.name().startsWith(query)).thenComparing(CommandInfo::name))
                .limit(Autocompleter.MAX_CHOICES)
                .map(cmd -> new Command.Choice(choiceName(cmd), cmd.name()))
                .toList();
    }

    static String choiceName(CommandInfo cmd) {
        final var text = "/" + cmd.name() + " ~ " + cmd.description();
        return text.length() <= MAX_CHOICE_NAME ? text : text.substring(0, MAX_CHOICE_NAME - 1) + "...";
    }

    void appendSubcommand(StringBuilder builder, String path, CommandInfo.SubcommandInfo subcmd) {
        builder.append("* `")
                .append(path)
                .append("` - **")
                .append(subcmd.description())
                .append("**\n");

        for (var opt: subcmd.options()) {
            builder.append("\t* `")
                    .append(opt.required() ? '<' : '[')
                    .append(opt.name())
                    .append(":")
                    .append(opt.type().name().toLowerCase(Locale.ROOT))
                    .append(opt.required() ? '>' : ']')
                    .append("` - ")
                    .append(opt.description())
                    .append('\n');
        }
    }
}
