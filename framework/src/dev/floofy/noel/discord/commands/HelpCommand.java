/// 🐾✨ Noel: Discord bot made to manage my servers made in Java
/// Copyright 2021-2025 Noel Towa <cutie@floofy.dev>
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

package dev.floofy.noel.discord.commands;

import com.google.inject.Inject;
import com.google.inject.Provider;
import dev.floofy.noel.discord.framework.AbstractCommand;
import dev.floofy.noel.discord.framework.CommandContext;
import dev.floofy.noel.discord.framework.CommandOption;
import dev.floofy.noel.discord.framework.Framework;
import dev.floofy.noel.discord.framework.annotations.Command;
import dev.floofy.noel.discord.framework.annotations.Option;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Command(
        name = "help",
        description = "Shows the help menu or showcases a command's information"
)
public class HelpCommand extends AbstractCommand {
    @Option(name = "command", description = "The command to get information on")
    private CommandOption command = new CommandOption(String.class);

    private final Provider<Framework> framework;

    @Inject
    public HelpCommand(Provider<Framework> framework) {
        registerOptions();

        this.framework = framework;
    }

    @Override
    public void execute(CommandContext ctx) {
        final String cmd = ctx.getOption(command);
        if (cmd == null) {
            final var display = renderHelp(ctx);

            ctx
                .getInteraction()
                    .reply(new MessageCreateBuilder().useComponentsV2().addComponents(List.of(display)).build()).queue();

            return;
        }

        final var commands = framework.get().getCommands();
        final var command = commands.stream()
                .filter(f -> f.getInfo().name().equals(cmd))
                .findAny();

        if (command.isEmpty()) {
            ctx.getInteraction().reply(String.format(":question: **--=--** Unknown command provided: %s", cmd)).setEphemeral(true).queue();
            return;
        }

        var cmd2 = command.get();

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

    private TextDisplay renderHelp(CommandContext ctx) {
        final var msg = new StringBuilder();
        msg.append("## Help Menu\n");

        final var maxCmdLength = framework.get().getCommands().stream()
                .map(cmd -> cmd.getInfo().name())
                .mapToInt(String::length)
                .max()
                .orElse(0);

        for (var cmd: framework.get().getCommands()) {
            msg.append("* `/");
            msg.append(cmd.getInfo().name());
            msg.append("`");
            msg.repeat(' ', maxCmdLength);
            msg.append("**");
            msg.append(cmd.getInfo().description());
            msg.append("**");
            msg.append('\n');
        }

        return TextDisplay.of(msg.toString());
    }
}
