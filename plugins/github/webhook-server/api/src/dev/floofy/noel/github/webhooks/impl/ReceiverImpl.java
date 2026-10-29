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

package dev.floofy.noel.github.webhooks.impl;

import com.google.inject.Inject;
import dev.floofy.noel.github.webhooks.api.EventFormatter;
import dev.floofy.noel.github.webhooks.api.Receiver;
import dev.floofy.noel.github.webhooks.api.Signatures;
import dev.floofy.noel.github.webhooks.api.SubscriptionStore;
import dev.floofy.noel.github.webhooks.models.Subscription;
import dev.floofy.noel.http.server.Request;
import dev.floofy.noel.http.server.Response;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class ReceiverImpl implements Receiver {
    private static final String X_GITHUB_EVENT = "X-GitHub-Event";
    private static final Logger LOG = LoggerFactory.getLogger(ReceiverImpl.class);

    private final JsonMapper mapper = JsonMapper.shared();
    private final EventFormatter formatter;
    private final ExecutorService executor;
    private final SubscriptionStore store;
    private final JDA jda;

    @Inject
    ReceiverImpl(JDA jda, SubscriptionStore store, ExecutorService executor, EventFormatter formatter) {
        this.formatter = formatter;
        this.executor = executor;
        this.store = store;
        this.jda = jda;
    }

    @Override
    public void handle(Request req, Response res) {
        final var id = req.getQueryParameter("id").orElse(null);
        final var event = req.getHeader(X_GITHUB_EVENT).orElse(null);
        final var signature = req.getHeader("X-Hub-Signature-256").orElse(null);
        final var delivery = req.getHeader("X-GitHub-Delivery").orElse("?");
        final var body = req.getBody();

        if (id == null || event == null) {
            reply(
                    res,
                    HttpResponseStatus.BAD_REQUEST,
                    "expected `?id` query parameter and `%s` header to be present".formatted(X_GITHUB_EVENT));
            return;
        }

        try {
            executor.execute(() -> process(res, id, event, signature, delivery, body));
        } catch (RejectedExecutionException ex) {
            reply(res, HttpResponseStatus.SERVICE_UNAVAILABLE, "shutting down; redeliver later");
        }
    }

    private static void reply(Response response, HttpResponseStatus status, String message) {
        reply(response, status, false, message);
    }

    private static void reply(Response response, HttpResponseStatus status, boolean success, String message) {
        try {
            response.setStatus(status).sendJSON(Map.<String, Object>of("success", success, "data", message));
        } catch (RuntimeException ex) {
            LOG.debug("couldn't reply to a webhook delivery", ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String text(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? null : node.asString();
    }

    private void process(Response res, String id, String event, String signature, String delivery, byte[] body) {
        try {
            final var found = this.store.find(id);
            if (found.isEmpty()) {
                reply(res, HttpResponseStatus.NOT_FOUND, "unknown webhook: it may have been removed");
                return;
            }

            final var subscription = found.get();
            if (!Signatures.verify(subscription.secret(), body, signature)) {
                LOG.warn("rejected delivery [{}] for webhook [{}]: bad or missing signature", delivery, id);
                reply(res, HttpResponseStatus.UNAUTHORIZED, "signature doesn't match this webhook's secret");

                return;
            }

            LOG.trace("BODY: {}", new String(body, StandardCharsets.UTF_8));
            final JsonNode payload;
            try {
                payload = mapper.readTree(body);
            } catch (RuntimeException ex) {
                reply(res, HttpResponseStatus.BAD_REQUEST, "body isn't JSON; set the content type to application/json");
                return;
            }

            if (event.equals("ping")) {
                formatter.format(event, payload).ifPresent(msg -> post(subscription, msg));
                reply(res, HttpResponseStatus.OK, true, "pong");

                return;
            }

            final var repo = payload.path("repository").path("full_name");
            if (repo == null) {
                reply(res, HttpResponseStatus.NOT_ACCEPTABLE, "webhook payload is missing `repository.full_path`");
                return;
            }

            if (!repo.isString()) {
                reply(res, HttpResponseStatus.NOT_ACCEPTABLE, "`repository.full_path` must be a string");
                return;
            }

            if (!subscription.accepts(event, repo.asString())) {
                reply(
                        res,
                        HttpResponseStatus.ACCEPTED,
                        true,
                        "ignored: event or repository not chosen for this webhook");

                return;
            }

            final var message = formatter.format(event, payload);
            if (message.isEmpty()) {
                reply(res, HttpResponseStatus.ACCEPTED, true, "ignored: nothing to post for this action");
                return;
            }

            reply(res, HttpResponseStatus.ACCEPTED, true, "posted");
            post(subscription, message.get());
        } catch (Exception ex) {
            LOG.error("failed to handle delivery {} ({}) for webhook {}", delivery, event, id, ex);
            reply(res, HttpResponseStatus.INTERNAL_SERVER_ERROR, "internal error; check Noel's logs");
        }
    }

    private void post(Subscription subscription, MessageCreateData message) {
        final var channel = jda.getChannelById(GuildMessageChannel.class, subscription.channelId());
        if (channel == null || channel.getGuild().getIdLong() != subscription.guildId()) {
            LOG.warn(
                    "webhook {}: channel {} is gone or not visible; not posting",
                    subscription.id(),
                    subscription.channelId());
            return;
        }

        channel.sendMessage(message)
                .setAllowedMentions(List.of())
                .setSuppressEmbeds(true)
                .queue(_ -> {}, error -> {
                    LOG.error(
                            "webhook {}: failed to post to channel {}:",
                            subscription.id(),
                            subscription.channelId(),
                            error);
                });
    }
}
