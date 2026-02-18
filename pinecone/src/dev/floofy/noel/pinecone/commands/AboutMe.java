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
import dev.floofy.noel.BuildInfo;
import dev.floofy.noel.pinecone.AbstractSlashCommand;
import dev.floofy.noel.pinecone.CommandContext;
import dev.floofy.noel.pinecone.annotations.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.SelfUser;

import java.awt.*;

@SlashCommand(name = "about", description = "Shows general information about this bot")
public class AboutMe extends AbstractSlashCommand {
    @Override
    public void execute(CommandContext context) {
        final SelfUser self = context.getSelfUser();
        final var modules = Noel.getInstance().getModules();

        final MessageEmbed embed = new EmbedBuilder()
                .setAuthor(String.format("%s#%s", self.getName(), self.getDiscriminator()))
                .setDescription("I am a simple utility bot that helps aid running this server")
                .setColor(Color.decode("#f4b5d5"))
                .addField("Modules", String.format("%d available", modules.size()), false)
                .addField("Versions", String.format("<a:noel:843673536392724500> **%s+%s** | JDA: **%s** | Java: **%s** [%s]", BuildInfo.getVersion(), BuildInfo.getGitCommit(), JDAInfo.VERSION, System.getProperty("java.version"), System.getProperty("java.vendor")), false)
                .setFooter("| https://github.com/auguwu/Noel", self.getAvatarUrl())
                .build();

        context.replyEmbeds(embed).setEphemeral(true).queue();
    }
}
