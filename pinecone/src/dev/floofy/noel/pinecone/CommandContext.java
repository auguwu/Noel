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

import dev.floofy.noel.Pinecone;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface CommandContext {
    /// Returns the [slash command interaction][SlashCommandInteraction] that is associated
    /// when a command was executed.
    @NotNull
    SlashCommandInteraction getInteraction();

    /// Returns an instance of a [pinecone][Pinecone].
    @NotNull
    Pinecone getPinecone();

    /// Returns the [Guild] that is associated with the slash command interaction.
    @Nullable
    default Guild getGuild() {
        return getInteraction().getGuild();
    }

    /// Returns the current [JDA] instance.
    @NotNull
    default JDA getJDA() {
        return getInteraction().getJDA();
    }

    /// Forwarded method: [`JDA#getSelfUser()`][JDA#getSelfUser()]
    @NotNull
    default SelfUser getSelfUser() {
        return getJDA().getSelfUser();
    }

    /// Forwarded method: [`SlashCommandInteraction#deferReply()`][SlashCommandInteraction#deferReply()]
    @NotNull
    @CheckReturnValue
    default ReplyCallbackAction deferReply() {
        return getInteraction().deferReply();
    }

    /// Forwarded method: [`SlashCommandInteraction#deferReply(boolean)`][SlashCommandInteraction#deferReply(boolean)]
    @NotNull
    @CheckReturnValue
    default ReplyCallbackAction deferReply(boolean isEphemeral) {
        return deferReply().setEphemeral(isEphemeral);
    }

    /// Forwarded method: [`SlashCommandInteraction#reply(String)`][SlashCommandInteraction#reply(String)]
    @NotNull
    @CheckReturnValue
    default ReplyCallbackAction reply(@NotNull String message) {
        return getInteraction().reply(message);
    }

    /// Forwarded method: [`SlashCommandInteraction#reply(MessageCreateData)`][SlashCommandInteraction#reply(MessageCreateData)]
    @NotNull
    @CheckReturnValue
    default ReplyCallbackAction reply(@NotNull MessageCreateData message) {
        return getInteraction().reply(message);
    }

    /// Forwarded method: [`SlashCommandInteraction#reply(String, Object...)`][SlashCommandInteraction#reply(String, Object...)]
    @NotNull
    @CheckReturnValue
    default ReplyCallbackAction replyFormat(@NotNull String format, Object... args) {
        return getInteraction().replyFormat(format, args);
    }

    /// Forwarded method: [`SlashCommandInteraction#reply(Collection<? extends MessageEmbed>)`][SlashCommandInteraction#replyEmbeds(Collection<? extends MessageEmbed>)]
    @NotNull
    @CheckReturnValue
    default ReplyCallbackAction replyEmbeds(@NotNull Collection<? extends MessageEmbed> embeds) {
        return getInteraction().replyEmbeds(embeds);
    }

    /// Forwarded method: [`SlashCommandInteraction#replyEmbeds(MessageEmbed, MessageEmbed...)`][SlashCommandInteraction#replyEmbeds(MessageEmbed, MessageEmbed...)]
    @NotNull
    @CheckReturnValue
    default ReplyCallbackAction replyEmbeds(@NotNull MessageEmbed embed, @NotNull MessageEmbed... embeds) {
        return getInteraction().replyEmbeds(embed, embeds);
    }
}

/*
    @Nonnull
    @CheckReturnValue
    default ReplyCallbackAction replyEmbeds(@Nonnull Collection<? extends MessageEmbed> embeds) {
        return (ReplyCallbackAction)this.deferReply().addEmbeds(embeds);
    }

    @Nonnull
    @CheckReturnValue
    default ReplyCallbackAction replyEmbeds(@Nonnull MessageEmbed embed, @Nonnull MessageEmbed... embeds) {
        Checks.notNull(embed, "MessageEmbed");
        Checks.noneNull(embeds, "MessageEmbed");
        return (ReplyCallbackAction)((ReplyCallbackAction)this.deferReply().addEmbeds(new MessageEmbed[]{embed})).addEmbeds(embeds);
    }

    @Nonnull
    @CheckReturnValue
    default ReplyCallbackAction replyComponents(@Nonnull Collection<? extends MessageTopLevelComponent> components) {
        return (ReplyCallbackAction)this.deferReply().setComponents(components);
    }

    @Nonnull
    @CheckReturnValue
    default ReplyCallbackAction replyComponents(@Nonnull MessageTopLevelComponent component, @Nonnull MessageTopLevelComponent... other) {
        Checks.notNull(component, "MessageTopLevelComponent");
        Checks.noneNull(other, "MessageTopLevelComponents");
        return this.replyComponents(Helpers.mergeVararg(component, other));
    }

    @Nonnull
    @CheckReturnValue
    default ReplyCallbackAction replyComponents(@Nonnull ComponentTree<? extends MessageTopLevelComponent> tree) {
        Checks.notNull(tree, "ComponentTree");
        return this.replyComponents(tree.getComponents());
    }

    @Nonnull
    @CheckReturnValue
    default ReplyCallbackAction replyFormat(@Nonnull String format, @Nonnull Object... args) {
        Checks.notNull(format, "Format String");
        return this.reply(String.format(format, args));
    }
 */
