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

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import dev.floofy.noel.Pinecone;
import java.util.Collection;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

/// Everything a command needs while handling one invocation.
///
/// Passed to [AbstractSlashCommand#execute(CommandContext)], to every
/// [`@Subcommand`][dev.floofy.noel.pinecone.annotations.Subcommand]
/// method, and to [CommandInterceptor]s. A context belongs to a single invocation; don't keep it after the command
/// returns.
///
/// ## Replying
/// Discord expects a response to every interaction **within 3 seconds**, or the
/// user sees "The application did not respond". Reply directly when the work is
/// quick. For anything slower, [defer][#deferReply()] first, then send the result
/// through the interaction hook:
///
/// ```java
/// context.deferReply().queue();
/// var result = slowLookup();
/// context.getInteraction().getHook().sendMessage(result).queue();
/// ```
///
/// An interaction can only be replied to once. After a reply or a defer, every
/// further message must go through `getInteraction().getHook()`; calling
/// [#reply(String)] again fails.
///
/// The `reply` methods return a [ReplyCallbackAction], which does nothing until
/// it is queued. A reply that is never `.queue()`d is never sent.
public interface CommandContext {
    /// Returns the interaction being handled.
    SlashCommandInteraction getInteraction();

    /// Returns the slash command framework, for example to list commands.
    Pinecone getPinecone();

    /// Returns the guild the command was run in.
    /// @return the guild, or `null` if the command was run in DMs
    default Guild getGuild() {
        return getInteraction().getGuild();
    }

    /// Returns the JDA instance that received the interaction.
    default JDA getJDA() {
        return getInteraction().getJDA();
    }

    default User getUser() {
        return getInteraction().getUser();
    }

    default Member getMember() {
        return getInteraction().getMember();
    }

    /// Returns the bot's own user. Shorthand for [JDA#getSelfUser()].
    /// @return the bot user
    default SelfUser getSelfUser() {
        return getJDA().getSelfUser();
    }

    /// Acknowledges the interaction without replying yet, so Discord shows
    /// "thinking…" and the command has up to 15 minutes to respond through the
    /// interaction hook. Shorthand for [SlashCommandInteraction#deferReply()].
    ///
    /// @return the action; must be queued
    @CheckReturnValue
    default ReplyCallbackAction deferReply() {
        return getInteraction().deferReply();
    }

    /// Like [#deferReply()], choosing whether the eventual response is visible only
    /// to the user who ran the command.
    ///
    /// @param isEphemeral `true` to make the response visible only to that user
    /// @return the action; must be queued
    @CheckReturnValue
    default ReplyCallbackAction deferReply(boolean isEphemeral) {
        return deferReply().setEphemeral(isEphemeral);
    }

    /// Replies with a text message. Shorthand for [SlashCommandInteraction#reply(String)].
    /// @param message the message, up to 2000 characters
    /// @return the action; must be queued
    @CheckReturnValue
    default ReplyCallbackAction reply(String message) {
        return getInteraction().reply(message);
    }

    /// Replies with a prepared message, for example one using components. Shorthand
    /// for [SlashCommandInteraction#reply(MessageCreateData)].
    ///
    /// @param message the message
    /// @return the action; must be queued
    @CheckReturnValue
    default ReplyCallbackAction reply(MessageCreateData message) {
        return getInteraction().reply(message);
    }

    /// Replies with a message built by [String#format(String,Object...)].
    /// Shorthand for [SlashCommandInteraction#replyFormat(String,Object...)].
    ///
    /// @param format the format string
    /// @param args   the format arguments
    /// @return the action; must be queued
    @CheckReturnValue
    @FormatMethod
    default ReplyCallbackAction replyFormat(@FormatString String format, Object... args) {
        return getInteraction().replyFormat(format, args);
    }

    /// Replies with embeds. Shorthand for [SlashCommandInteraction#replyEmbeds(Collection)].
    /// @param embeds the embeds, up to 10
    /// @return the action; must be queued
    @CheckReturnValue
    default ReplyCallbackAction replyEmbeds(Collection<? extends MessageEmbed> embeds) {
        return getInteraction().replyEmbeds(embeds);
    }

    /// Replies with one or more embeds. Shorthand for
    /// [SlashCommandInteraction#replyEmbeds(MessageEmbed,MessageEmbed...)].
    ///
    /// @param embed  the first embed
    /// @param embeds more embeds, up to 10 in total
    /// @return the action; must be queued
    @CheckReturnValue
    default ReplyCallbackAction replyEmbeds(MessageEmbed embed, MessageEmbed... embeds) {
        return getInteraction().replyEmbeds(embed, embeds);
    }
}
