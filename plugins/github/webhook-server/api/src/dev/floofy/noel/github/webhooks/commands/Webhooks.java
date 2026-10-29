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

package dev.floofy.noel.github.webhooks.commands;

import static net.dv8tion.jda.api.utils.MarkdownUtil.*;

import com.google.inject.Inject;
import dev.floofy.Noel;
import dev.floofy.noel.Setting;
import dev.floofy.noel.Settings;
import dev.floofy.noel.github.webhooks.api.Event;
import dev.floofy.noel.github.webhooks.api.Signatures;
import dev.floofy.noel.github.webhooks.api.SubscriptionStore;
import dev.floofy.noel.github.webhooks.models.Subscription;
import dev.floofy.noel.pinecone.annotations.*;
import dev.floofy.noel.pinecone.api.AbstractSlashCommand;
import dev.floofy.noel.pinecone.api.AutocompleteContext;
import dev.floofy.noel.pinecone.api.Autocompleter;
import dev.floofy.noel.pinecone.api.CommandContext;
import java.time.Clock;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.MarkdownUtil;

@SlashCommand(
        name = "webhooks",
        description = "GitHub webhooks that post to this server",
        defaultPermissions = {Permission.MANAGE_SERVER})
@GuildRegistration(WebhookGuildScope.class)
public final class Webhooks extends AbstractSlashCommand {
    static final Setting<String> PUBLIC_URL = Setting.string("plugins.github.webhooks.public_url");

    @Inject
    private SubscriptionStore store;

    @Override
    public void execute(CommandContext cx) {}

    @Subcommand(description = "list this server's GitHub webhooks")
    public void list(CommandContext cx) {
        final var guild = cx.getGuild();
        if (guild == null) {
            return;
        }

        final var webhooks = store.list(guild.getIdLong());
        if (webhooks.isEmpty()) {
            cx.reply(":mailbox_with_no_mail: **|> No webhooks are configured! Add one with `/webhooks config add`**")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        var embed = new EmbedBuilder()
                .setColor(Noel.EMBED_COLOUR)
                .setAuthor("| GitHub Webhooks", null, cx.getSelfUser().getAvatarUrl());

        for (var webhook : webhooks) {
            //noinspection StringBufferReplaceableByString
            final var message = new StringBuilder()
                    .append("* ")
                    .append(webhook.repository() == null ? "*any repository*" : bold(webhook.repository()))
                    .append(" ~> <#")
                    .append(webhook.channelId())
                    .append(">")
                    .append('\n')
                    .append("> Listening to Events: ")
                    .append(
                            webhook.events().isEmpty()
                                    ? "all events"
                                    : String.join(
                                            ", ",
                                            webhook.events().stream()
                                                    .map(MarkdownUtil::bold)
                                                    .toList()))
                    .toString();

            embed = embed.addField(webhook.id(), message, true);
        }

        cx.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    @Subcommand(description = "List the GitHub events Noel can post")
    public void events(CommandContext cx) {
        final var out = new StringBuilder("### Supported Events\n");
        for (var ev : Event.values()) {
            out.append("* ")
                    .append(monospace(ev.id()))
                    .append(": ")
                    .append(ev.description())
                    .append(ev.isDefault() ? " *(default)*" : "")
                    .append('\n');
        }

        out.append("\n"
                + "-# A webhook only posts events that are chosen both here and in the Webhooks settings on a"
                + " GitHub repository");

        cx.reply(out.toString()).setEphemeral(true).queue();
    }

    @SubcommandGroup(name = "config", description = "Add, change or remove GitHub webhooks")
    public static final class Config {
        private static final Set<ChannelType> ALLOWED_CHANNEL_TYPES = Set.of(ChannelType.TEXT, ChannelType.NEWS);
        private static final Pattern REPOSITORY = Pattern.compile("[A-Za-z0-9-]{1,39}/[A-Za-z0-9._-]{1,100}");

        @Inject
        private SubscriptionStore store;

        @Inject
        private Settings settings;

        @Inject
        private Clock clock;

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        @Subcommand(description = "Adds a GitHub webhook that posts to a channel")
        public void add(
                CommandContext cx,
                @Option(
                                name = "channel",
                                description = "channel to post events in",
                                required = true,
                                channelTypes = {ChannelType.TEXT, ChannelType.NEWS},
                                type = OptionType.CHANNEL)
                        GuildChannelUnion channel,
                @Option(name = "repository", description = "only post events from this repository (owner/name)")
                        Optional<String> repository,
                @Option(
                                name = "events",
                                description = "Events to post, comma-separated (default: push, pull_request, issues,"
                                        + " release)")
                        Optional<String> events) {
            final var guild = cx.getGuild();
            if (guild == null) {
                return;
            }

            final var baseUrl = settings.get(PUBLIC_URL).map(url -> url.replaceAll("/+$", ""));
            if (baseUrl.isEmpty()) {
                cx.replyFormat(
                                ":warning: **|> `%s` isn't configured! there is no URL to give to GitHub. this is a"
                                        + " internal issue, report this to the owner.**",
                                PUBLIC_URL.key())
                        .setEphemeral(true)
                        .queue();
                return;
            }

            final var problem = checkChannel(guild, channel);
            if (problem.isPresent()) {
                cx.replyFormat(":x: **|> %s**", problem.get())
                        .setEphemeral(true)
                        .queue();
                return;
            }

            String repo = null;
            if (repository.isPresent()) {
                repo = repository.map(String::strip).get();
                if (!REPOSITORY.matcher(repo).matches()) {
                    cx.replyFormat(
                                    ":x: **|> `%s` is not a valid repository! use `owner/name` (e.g., `auguwu/Noel`)**",
                                    repo)
                            .setEphemeral(true)
                            .queue();
                    return;
                }
            }

            final Set<String> chosen;
            try {
                chosen = events.map(Event::parseList).orElse(Set.of());
            } catch (IllegalArgumentException ex) {
                cx.replyFormat(":x: **|> %s**", ex.getMessage())
                        .setEphemeral(true)
                        .queue();
                return;
            }

            if (store.count(guild.getIdLong()) >= SubscriptionStore.MAX_PER_GUILD) {
                cx.replyFormat(
                                ":x: **|> this server already has %d webhooks; remove one first.**",
                                SubscriptionStore.MAX_PER_GUILD)
                        .setEphemeral(true)
                        .queue();
                return;
            }

            final var webhook = new Subscription(
                    Signatures.newId(),
                    guild.getIdLong(),
                    channel.getIdLong(),
                    repo,
                    chosen,
                    Signatures.newSecret(),
                    cx.getUser().getIdLong(),
                    clock.instant());

            store.save(webhook);
            cx.reply(setupInstructions(baseUrl.get(), webhook, "created"))
                    .setEphemeral(true)
                    .queue();
        }

        @Subcommand(description = "Removes a GitHub webhook")
        public void remove(
                CommandContext cx, @Option(name = "id", description = "webhook id", required = true) String id) {
            final var guild = cx.getGuild();
            if (guild == null) {
                return;
            }

            final var removed = store.delete(guild.getIdLong(), id.strip());
            cx.reply(
                            removed
                                    ? String.format(
                                            ":wastebucket: **|> Webhook `%s` has been removed!**\n"
                                                    + "-# Delete the webhook from GitHub as well! Deliveries will fail"
                                                    + " with 404.",
                                            id)
                                    : unknown(id))
                    .setEphemeral(true)
                    .queue();
        }

        @Subcommand(description = "changes which events a webhook posts")
        public void events(
                CommandContext cx,
                @Option(name = "id", description = "webhook id", required = true) String id,
                @Option(name = "events", description = "events to post", required = true) String events) {
            final var guild = cx.getGuild();
            if (guild == null) {
                return;
            }

            final Set<String> chosen;
            try {
                chosen = Event.parseList(events);
            } catch (IllegalArgumentException ex) {
                cx.replyFormat(":x: **|> %s**", ex.getMessage())
                        .setEphemeral(true)
                        .queue();
                return;
            }

            final var updated = store.setEvents(guild.getIdLong(), id.strip(), chosen);
            cx.reply(
                            updated
                                    ? String.format(
                                            ":white_check_mark: **|> Webhook `%s` now posts: %s.**\n"
                                                    + "-# Make sure they're selected on GitHub to receive!",
                                            id, String.join(", ", chosen.isEmpty() ? Event.defaults() : chosen))
                                    : unknown(id))
                    .setEphemeral(true)
                    .queue();
        }

        @Subcommand(description = "moves a webhook to another channel")
        public void channel(
                CommandContext cx,
                @Option(name = "id", description = "webhook id", required = true) String id,
                @Option(
                                name = "channel",
                                description = "channel to post events in",
                                type = OptionType.CHANNEL,
                                required = true,
                                channelTypes = {ChannelType.TEXT, ChannelType.NEWS})
                        GuildChannelUnion channel) {
            final var guild = cx.getGuild();
            if (guild == null) {
                return;
            }

            final var problem = checkChannel(guild, channel);
            if (problem.isPresent()) {
                cx.replyFormat(":x: **|> %s**", problem.get())
                        .setEphemeral(true)
                        .queue();
                return;
            }

            final var updated = store.setChannel(guild.getIdLong(), id.strip(), channel.getIdLong());
            cx.reply(
                            updated
                                    ? String.format(
                                            ":white_check_mark: **|> Webhook `%s` now posts to %s**",
                                            id.strip(), channel.getAsMention())
                                    : unknown(id))
                    .setEphemeral(true)
                    .queue();
        }

        @Subcommand(name = "rotate-secret", description = "give a new webhook a new secret, if the old one leaked")
        public void rotate(
                CommandContext cx, @Option(name = "id", description = "webhook id", required = true) String id) {
            final var guild = cx.getGuild();
            if (guild == null) {
                return;
            }

            final var secret = Signatures.newSecret();
            if (!store.setSecret(guild.getIdLong(), id.strip(), secret)) {
                cx.reply(unknown(id)).setEphemeral(true).queue();
                return;
            }

            cx.replyFormat(
                            ":key: **|> New secret for webhook `%s`: ||`%s`||**\n"
                                + "-# Paste it into the webhook's **Secret** field on GitHub. Until you do, deliveries"
                                + " will fail with `401` and nothing will be posted.",
                            id.strip(), secret)
                    .setEphemeral(true)
                    .queue();
        }

        @Autocomplete("id")
        public List<Command.Choice> autocompleteId(AutocompleteContext cx, String typed) {
            final var guild = cx.getGuild();
            if (guild == null) {
                return List.of();
            }

            final var query = typed.strip().toLowerCase(Locale.ROOT);
            final var out = new ArrayList<Command.Choice>();
            for (var webhook : store.list(guild.getIdLong())) {
                final var channel = guild.getGuildChannelById(webhook.channelId());
                final var label = String.format(
                        "%s -> #%s (%s)",
                        webhook.repository() == null ? "any repository" : webhook.repository(),
                        channel == null ? "deleted-channel" : channel.getName(),
                        webhook.id());

                if (query.isEmpty() || label.toLowerCase(Locale.ROOT).contains(query)) {
                    out.add(new Command.Choice(
                            label.length() > 100 ? String.format("%s...", label.substring(0, 99)) : label,
                            webhook.id()));
                }
            }

            return out;
        }

        @Autocomplete("events")
        public List<Command.Choice> completeEvents(AutocompleteContext cx, String typed) {
            final int comma = typed.lastIndexOf(',');
            final var done = comma < 0 ? "" : typed.substring(0, comma + 1);
            final var partial = typed.substring(comma + 1).strip().toLowerCase(Locale.ROOT);

            final var already = Arrays.stream(done.split(","))
                    .map(name -> name.strip().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());

            final var prefix = done.isBlank() ? "" : String.format("%s ", done.strip());
            final var names = new LinkedHashSet<String>();
            if (done.isBlank()) {
                names.add("default");
            }

            Arrays.stream(Event.values()).map(Event::id).forEach(names::add);
            return names.stream()
                    .filter(name -> !already.contains(name))
                    .filter(name -> name.startsWith(partial))
                    .map(name -> prefix + name)
                    .filter(value -> value.length() <= 100)
                    .limit(Autocompleter.MAX_CHOICES)
                    .map(value -> new Command.Choice(value, value))
                    .toList();
        }

        private static Optional<String> checkChannel(Guild guild, GuildChannelUnion channel) {
            if (!ALLOWED_CHANNEL_TYPES.contains(channel.getType())) {
                return Optional.of(String.format("%s isn't a text or announcement channel", channel.getAsMention()));
            }

            final var self = guild.getSelfMember();
            if (!self.hasPermission(channel, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)) {
                return Optional.of(String.format("I cannot send messages in %s", channel.getAsMention()));
            }

            return Optional.empty();
        }

        private static String unknown(String id) {
            return String.format(
                    ":question: **|> this server has no webhook `%s`? view `/webhooks list` again.**", id.strip());
        }

        static String setupInstructions(String baseUrl, Subscription webhook, String verb) {
            final var url = String.format("%s/webhooks/github?id=%s", baseUrl, webhook.id());
            return String.format(
                    """
                    :white_check_mark: | Webhook `%s` has been %s to post in <#%d>%s
                    \s
                    ### Instructions
                    In GitHub, open the repository (or organization)'s Webhooks settings (Settings ~> Webhooks ~> Add webhook) and fill in the properties:
                    * **Payload URL**:                   `%s`
                    * **Content Type**:                  `application/json`
                    * **Secret** (shown only once here): ||`%s`||
                    * **Events**: Click on *"Let me select individual events"* and tick: %s
                    \s
                    -# :warning: WARNING: this will be the only time the webhook secret is shown! if it is lost, run `/webhooks config rotate-secret`.
                    \s\
                    """.stripIndent(),
                    webhook.id(),
                    verb,
                    webhook.channelId(),
                    webhook.repository() == null ? "" : String.format(" for %s", bold(webhook.repository())),
                    url,
                    webhook.secret(),
                    String.join(", ", webhook.effectiveEvents()));
        }
    }
}
