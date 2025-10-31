package dev.floofy.noel.discord.commands;

import dev.floofy.noel.discord.framework.AbstractCommand;
import dev.floofy.noel.discord.framework.CommandContext;
import dev.floofy.noel.discord.framework.annotations.Command;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Command(name = "ping", description = "pong!")
public class PingCommand extends AbstractCommand {
    private static final List<String> RESPONSES = List.of(
        ":thinking: *...what am i doing with my life?*",

        // i hate my life
        "yiffin' here, yiffin' there",
        "fox hole my beloved",
        "owo da uwu"
    );

    @Override
    public void execute(CommandContext ctx) {
        final var start = System.currentTimeMillis();
        final var response = RESPONSES.get(ThreadLocalRandom.current().nextInt(RESPONSES.size()));

        ctx.getInteraction().reply(response).queue(hook -> {
            final var end = System.currentTimeMillis() - start;
            hook.editOriginal(String.format(":ping_pong: **^w^** %dms (%s)", end, response)).queue();
        });
    }
}
