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

package dev.floofy.noel.github.webhooks.api;

import static net.dv8tion.jda.api.utils.MarkdownSanitizer.*;
import static net.dv8tion.jda.api.utils.MarkdownUtil.*;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.floofy.Noel;
import dev.floofy.noel.github.webhooks.internals.Usernames;
import dev.floofy.noel.utils.Strings;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import tools.jackson.databind.JsonNode;

/// Turns a GitHub webhook payload into a Discord message.
///
/// Everything in a payload that a user wrote (commit messages, titles, branch
/// names, comments) is escaped, so it can't add formatting, links or mentions.
/// Mentions are also switched off when the message is sent, which is what
/// actually stops `@everyone` in a commit message from pinging anyone.
///
/// Links are wrapped in `<...>` so Discord doesn't add a preview for each one.
@Singleton
public final class EventFormatter {
    private static final Pattern GITHUB_LOGIN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9-]{0,38}");
    private static final Pattern HTML_COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    private static final int MAX_BODY = 1000;

    /// Discord's limit on a message's length.
    public static final int MAX_LENGTH = 2000;

    /// How many commits a push lists before "and N more".
    public static final int MAX_COMMITS = 5;
    public static final int MAX_NOTES = 1500;

    private final Usernames usernames;

    @Inject
    EventFormatter(Usernames usernames) {
        this.usernames = usernames;
    }

    /// Formats a delivery.
    /// @param event   the `X-GitHub-Event` header, such as `push`
    /// @param payload the parsed body
    /// @return the message, or empty if this delivery shouldn't be posted, such
    ///         as an issue being labeled or a tag push (posted as `create`)
    public Optional<MessageCreateData> format(String event, JsonNode payload) {
        final var message = switch (event) {
            case "ping" -> ping(payload);
            case "push" -> push(payload);
            case "pull_request" -> pullRequest(payload);
            case "issues" -> issues(payload);
            case "release" -> release(payload);
            case "create" -> createOrDelete(payload, true);
            case "delete" -> createOrDelete(payload, false);
            case "star" -> "created".equals(text(payload, "action")) ? star(payload) : null;
            case "watch" -> "started".equals(text(payload, "action")) ? star(payload) : null;
            case "fork" -> fork(payload);
            //            case "workflow_run" -> workflowRun(payload);
            default -> null;
        };

        return Optional.ofNullable(message);
    }

    private MessageCreateData container(List<ContainerChildComponent> children) {
        Objects.requireNonNull(children, "children");
        return new MessageCreateBuilder()
                .useComponentsV2()
                .addComponents(Container.of(children).withAccentColor(Noel.EMBED_COLOUR))
                .build();
    }

    private MessageCreateData reply(String message) {
        Objects.requireNonNull(message, "message");
        return new MessageCreateBuilder().setContent(message).build();
    }

    @FormatMethod
    private MessageCreateData reply(@FormatString String format, Object... args) {
        return reply(String.format(Objects.requireNonNull(format, "format"), args));
    }

    private String text(JsonNode node, String... path) {
        var current = node;
        for (var field : path) {
            current = current.path(field);
        }

        return current.isMissingNode() || current.isNull() ? null : current.asString();
    }

    private String profile(
            String login, @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<String> name) {
        return profile(login, name, MarkdownUtil::monospace);
    }

    private String profile(
            String login,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<String> name,
            Function<String, String> style) {
        final var handle = GITHUB_LOGIN.matcher(login).matches()
                ? String.format("[%s](<https://github.com/%s>)", style.apply(String.format("@%s", login)), login)
                : style.apply(String.format("@%s", login));

        return name.map(n -> String.format("%s %s", escape(n), handle)).orElse(handle);
    }

    private String thumbnail(JsonNode payload) {
        final var owner = text(payload, "repository", "owner", "avatar_url");
        if (owner != null) {
            return owner;
        }

        final var organization = text(payload, "organization", "avatar_url");
        return organization != null ? organization : text(payload, "sender", "avatar_url");
    }

    private String actor(JsonNode payload) {
        return actor(payload, MarkdownUtil::bold);
    }

    private String actor(JsonNode payload, Function<String, String> style) {
        var sender = text(payload, "login");
        if (sender == null) {
            sender = text(payload, "username");
            if (sender == null) {
                sender = "someone";
            }
        }

        if (!sender.equals("someone")) {
            sender = profile(sender, usernames.of(sender));
        }

        return style.apply(sender);
    }

    private String repository(JsonNode payload) {
        return repository(payload, Function.identity());
    }

    private String repository(JsonNode payload, Function<String, String> style) {
        final var name = text(payload, "repository", "full_name");
        if (name == null) {
            return null;
        }

        return String.format("[%s](<%s>)", style.apply(escape(name)), text(payload, "repository", "html_url"));
    }

    private String link(String label, String url) {
        return link(label, url, Function.identity());
    }

    private String link(String label, String url, Function<String, String> style) {
        final var escaped = escape(label == null ? "?" : label);
        return url == null ? style.apply(escaped) : String.format("[%s](<%s>)", style.apply(escaped), url);
    }

    private String firstLine(String text, int max) {
        if (text == null) {
            return "";
        }

        final var line = text.strip().lines().findFirst().orElse("");
        return line.length() > max ? String.format("%s...", line.substring(0, max - 1)) : line;
    }

    private String numbered(JsonNode item) {
        final var label = firstLine(text(item, "title"), 80);
        final var num = text(item, "number");
        final var url = text(item, "html_url");

        return link(String.format("%s [#%s]", label, num), url);
    }

    /// Discord only accepts http(s) URLs of at most 512 characters on a link
    /// button, and rejects the whole message otherwise.
    private boolean isLinkable(String url) {
        //noinspection HttpUrlsUsage
        return url != null && url.length() <= 512 && (url.startsWith("https://") || url.startsWith("http://"));
    }

    private String commitAuthor(JsonNode author) {
        final var username = text(author, "username");
        final var gitName = text(author, "name");
        if (username == null) {
            return gitName == null ? null : escape(gitName);
        }

        final var name = usernames.of(username).or(() -> Optional.ofNullable(gitName));
        return profile(username, name);
    }

    private MessageCreateData ping(JsonNode payload) {
        final var repo = text(payload, "repository", "full_name");
        final var target = repo != null
                ? repository(payload)
                : bold(Objects.requireNonNull(text(payload, "organization", "login"), "organization.login"));

        return reply(":link: **| GitHub webhook has connected for repository %s!**", target);
    }

    private MessageCreateData push(JsonNode payload) {
        final var ref = text(payload, "ref");
        if (ref == null
                || !ref.startsWith("refs/heads/")
                || payload.path("deleted").asBoolean(false)) {
            return null;
        }

        final var commits = payload.path("commits");
        final int count = commits.size();
        if (count == 0) {
            return null;
        }

        final var pusher = text(payload, "sender", "login");
        final var branch = ref.substring("refs/heads/".length());
        final var children = new ArrayList<ContainerChildComponent>();
        children.add(Section.of(
                Thumbnail.fromUrl(thumbnail(payload)),
                TextDisplay.ofFormat("## %s", repository(payload)),
                TextDisplay.ofFormat(
                        "### :package: | %s %s %d commit%s to %s",
                        pusher == null ? bold("someone") : profile(pusher, Optional.of(pusher)),
                        payload.path("forced").asBoolean(false) ? "force-pushed" : "pushed",
                        count,
                        count == 1 ? "" : "s",
                        monospace(branch))));

        final var commitMessages = new StringBuilder();
        for (var i = 0; i < Math.min(count, MAX_COMMITS); i++) {
            final var commit = commits.get(i);
            final var sha = text(commit, "id");
            final var shortSha = sha == null ? "???????" : sha.substring(0, Math.min(7, sha.length()));

            commitMessages
                    .append('[')
                    .append(monospace(shortSha))
                    .append("](<")
                    .append(text(commit, "url"))
                    .append(">)")
                    .append(' ')
                    .append(escape(firstLine(text(commit, "message"), 72)));

            final var author = commitAuthor(commit.path("author"));
            if (author != null) {
                commitMessages.append(" ~ ").append(author);
            }

            commitMessages.append('\n');
        }

        if (count > MAX_COMMITS) {
            commitMessages.append("... and ").append(count - MAX_COMMITS).append(" more");
        }

        children.add(Separator.createDivider(Separator.Spacing.SMALL));
        children.add(TextDisplay.of(commitMessages.toString()));
        final var compareUrl = text(payload, "compare");
        if (isLinkable(compareUrl)) {
            children.add(ActionRow.of(Button.link(compareUrl, count == 1 ? "View Commit" : "Compare Changes")
                    .withEmoji(Emoji.fromUnicode("🔍"))));
        }

        return container(children);
    }

    private MessageCreateData pullRequest(JsonNode payload) {
        final var pr = payload.path("pull_request");
        final var action = text(payload, "action");

        if (action == null) {
            return null;
        }

        final var merged = pr.path("merged").asBoolean(false);
        final var actionEmoji = switch (action) {
            case "opened", "reopened" -> ":green_circle:";
            case "ready_for_review" -> ":eyes:";
            case "closed" -> merged ? ":purple_circle:" : ":red_circle:";
            case "converted_to_draft" -> ":pencil:";
            case "assigned" -> ":bust_in_silhoutte:";
            case "unassigned" -> ":busts_in_silhoutte:";
            default -> null;
        };

        if (actionEmoji == null) {
            return null;
        }

        final var issuer = actor(payload.path("sender"));
        final var actionMessage = switch (action) {
            case "opened" -> String.format("opened by %s", issuer);
            case "reopened" -> String.format("was reopened by %s", issuer);
            case "ready_for_review" -> String.format("was marked ready for review by %s", issuer);
            case "converted_to_draft" -> String.format("was converted to a draft by %s", issuer);
            case "closed" -> {
                if (merged) {
                    yield String.format("was merged by %s -> %s", actor(pr.path("merged_by")), monospace(Objects.requireNonNull(text(pr, "base", "ref"))));
                } else {
                    yield String.format("was closed by %s", issuer);
                }
            }

            case "assigned" -> String.format("was assigned to %s", actor(payload.path("assignee")));
            case "unassigned" -> String.format("was unassigned from %s", actor(payload.path("assignee")));
            default -> throw new AssertionError(action); // the emoji switch returned for everything else
        };

        return reply(
                "%s | Pull Request %s %s   [%s]",
                actionEmoji, numbered(pr), actionMessage, repository(payload));
    }

    private MessageCreateData issues(JsonNode payload) {
        final var action = text(payload, "action");
        if (action == null) {
            return null;
        }

        final var issue = payload.path("issue");
        if (action.equals("opened")) {
            final var author = issue.path("user");
            final var login = text(author, "login");
            final var header = TextDisplay.ofFormat("## %s", repository(payload));
            final var tag = TextDisplay.ofFormat(
                    "### :green_circle: | %s opened issue %s",
                    profile(login, usernames.of(login)),
                    link(
                            "#%s %s".formatted(text(issue, "number"), firstLine(text(issue, "title"), 80)),
                            text(issue, "html_url")));

            final var children = new ArrayList<ContainerChildComponent>();
            final var avatar = text(author, "avatar_url");
            if (avatar != null) {
                children.add(Section.of(
                        Thumbnail.fromUrl(avatar).withDescription("issuer's avatar on GitHub"), header, tag));
            } else {
                children.add(header);
                children.add(tag);
            }

            children.add(Separator.createDivider(Separator.Spacing.SMALL));
            children.add(TextDisplay.of(markdownBody(text(issue, "body"), MAX_BODY, "-# *No description provided.*")));

            final var url = text(issue, "html_url");
            if (isLinkable(url)) {
                children.add(ActionRow.of(Button.link(url, "View Issue")));
            }

            return container(children);
        }

        final var notPlanned = "not_planned".equals(text(issue, "state_reason"));
        final var actionEmoji = switch (action) {
            case "assigned" -> ":bust_in_silhouette:";
            case "unassigned" -> ":busts_in_silhouette:";
            case "closed" -> notPlanned ? ":white_circle:" : ":purple_circle:";
            case "reopened" -> ":green_circle:";
            case "locked" -> ":lock:";
            case "unlocked" -> ":unlock:";
            case "transferred" -> ":truck:";
            default -> null;
        };

        // We don't handle this specific issue action
        if (actionEmoji == null) {
            return null;
        }

        final String person = actor(payload.path("sender"));
        final var what = switch (action) {
            case "assigned" -> String.format("assigned to %s", actor(payload.path("assignee")));
            case "unassigned" -> String.format("unassigned to %s", actor(payload.path("assignee")));
            case "closed" ->
                String.format("%s by %s", notPlanned ? "closed as not planned" : "closed as completed", person);

            case "reopened" -> String.format("reopened by %s", person);
            case "locked" -> {
                final var reason = text(issue, "active_lock_reason");
                yield String.format(
                        "locked by %s%s", person, reason == null ? "" : String.format(" (%s)", escape(reason)));
            }

            case "unlocked" -> String.format("unlocked by %s", person);
            case "transferred" ->
                String.format(
                        "transferred to %s by %s",
                        link(
                                text(payload, "changes", "new_repository", "full_name"),
                                text(payload, "changes", "new_issue", "html_url")),
                        person);

            default -> throw new AssertionError(action);
        };

        return reply("%s | Issue %s was %s", actionEmoji, numbered(issue), what);
    }

    private static String markdownBody(String body, int max, String onEmpty) {
        if (body == null || body.isBlank()) {
            return "-# *No description provided.*";
        }

        var text = HTML_COMMENT
                .matcher(body)
                .replaceAll("")
                .replace("\r\n", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .strip();

        if (text.isEmpty()) {
            return onEmpty; // the template was all comments
        }

        if (text.length() > max) {
            text = text.substring(0, max - 1).stripTrailing() + "...";
        }

        return text;
    }

    private MessageCreateData release(JsonNode payload) {
        if (!"published".equals(text(payload, "action"))) {
            return null;
        }

        final var release = payload.path("release");
        final var tag = text(release, "tag_name");
        final var name = text(release, "name");

        final Function<JsonNode, String> assetsList = (assets) -> {
            if (!assets.isArray() || assets.isEmpty()) {
                return null;
            }

            final var names = new ArrayList<String>();
            for (var i = 0; i < Math.min(assets.size(), 5); i++) {
                final var asset = assets.get(i);
                names.add(String.format(
                        "%s (%s)",
                        link(text(asset, "name"), text(asset, "browser_download_url")),
                        Strings.toHumanSize(asset.path("size").asLong())));
            }

            final var more = assets.size() > 5 ? String.format(", and %d more", assets.size() - 5) : "";
            return String.format(
                    "-# %d asset%s: %s%s",
                    assets.size(), assets.size() == 1 ? "" : "s", String.join(", ", names), more);
        };

        final var header = TextDisplay.ofFormat("## %s", repository(payload));
        final var desc = TextDisplay.ofFormat(
                "### :rocket: | %s published %s%s%s",
                actor(release.path("author")),
                release.path("prerelease").asBoolean(false) ? "pre-release " : "",
                link(tag, text(release, "html_url")),
                name != null && !name.isBlank() ? String.format(": %s", name) : "");

        final var children = new ArrayList<ContainerChildComponent>();
        final var avatar = text(release, "author", "avatar_url");
        if (avatar != null) {
            children.add(
                    Section.of(Thumbnail.fromUrl(avatar).withDescription("release author's avatar"), header, desc));
        } else {
            children.add(header);
            children.add(desc);
        }

        children.add(Separator.createDivider(Separator.Spacing.SMALL));
        children.add(TextDisplay.of(markdownBody(text(release, "body"), MAX_NOTES, "-# *No release notes.*")));

        final var assets = assetsList.apply(release.path("assets"));
        if (assets != null) {
            children.add(TextDisplay.of(assets));
        }

        final var buttons = new ArrayList<Button>();
        final var url = text(release, "html_url");
        if (isLinkable(url)) {
            buttons.add(Button.link(url, "View Release"));
        }

        final var repo = text(payload, "repository", "html_url");
        if (tag != null && isLinkable(repo)) {
            final var tree = String.format("%s/tree/%s", repo, URLEncoder.encode(tag, StandardCharsets.UTF_8));
            if (isLinkable(tree)) {
                buttons.add(Button.link(tree, String.format("Browse %s", tag)));
            }
        }

        if (!buttons.isEmpty()) {
            children.add(ActionRow.of(buttons));
        }

        return container(children);
    }

    private MessageCreateData createOrDelete(JsonNode payload, boolean created) {
        final var type = text(payload, "ref_type");
        if (!"branch".equals(type) && !"tag".equals(type)) {
            return null;
        }

        return reply(
                "%s **| %s %s %s %s in %s**",
                created ? ":seedling:" : ":wastebucket:",
                actor(payload),
                created ? "created" : "deleted",
                type,
                monospace(Objects.requireNonNull(text(payload, "ref"))),
                repository(payload));
    }

    private MessageCreateData star(JsonNode payload) {
        if (!"created".equals(text(payload, "action")) || !"started".equals(text(payload, "action"))) {
            return null;
        }

        return reply(
                ":star: **| %s starred repository %s (at %d stars)**",
                actor(payload),
                repository(payload),
                payload.path("repository").path("stargazers_count").asInt());
    }

    private MessageCreateData fork(JsonNode payload) {
        return reply(
                ":fork_and_knife: **| %s forked %s -> %s**",
                actor(payload),
                repository(payload),
                link(text(payload, "forkee", "full_name"), text(payload, "forkee", "html_url")));
    }
}
