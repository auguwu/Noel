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

package dev.floofy.noel.github.impl;

import com.google.inject.Inject;
import dev.floofy.noel.Setting;
import dev.floofy.noel.Settings;
import dev.floofy.noel.github.api.Client;
import dev.floofy.noel.github.api.GitHubException;
import dev.floofy.noel.github.api.Method;
import dev.floofy.noel.github.api.Request;
import okhttp3.*;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.MissingNode;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public final class ClientImpl implements Client {
    private static final HttpUrl DEFAULT_BASE_URL = HttpUrl.get("https://api.github.com/");
    private static final Setting<HttpUrl> BASE_URL = Setting.of("plugins.github.base_url", Object.class).transform(raw -> switch (raw) {
        case String url -> HttpUrl.parse(url);
        default -> throw new IllegalArgumentException("expected string");
    });

    private static final Setting<String> TOKEN = Setting.string("plugins.github.token");
    private static final String USER_AGENT = "Noelbot (+https://github.com/auguwu/Noel)";
    private static final String ACCEPT = "application/vnd.github+json";
    private static final Logger LOG = LoggerFactory.getLogger(ClientImpl.class);

    /// The REST API version this client was written against.
    public static final String API_VERSION = "2026-03-10";

    private final ConcurrentMap<String, Route> routes = new ConcurrentHashMap<>();

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<String> token;
    private final OkHttpClient client;
    private final HttpUrl baseUrl;

    private final ObjectMapper mapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();

    private final Function<byte[], JsonNode> json =
            bytes -> bytes.length == 0 ? MissingNode.getInstance() : mapper.readTree(bytes);

    @Inject
    ClientImpl(OkHttpClient client, Settings settings) {
        this.baseUrl = settings.getOrDefault(BASE_URL, DEFAULT_BASE_URL);
        this.client = client;
        this.token = settings.get(TOKEN);
    }

    @Override
    public Request<JsonNode> request(Method method, String route, Map<String, ?> params) {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(route, "route");
        Objects.requireNonNull(params, "params");

        final var parsed = routes.computeIfAbsent(route, Route::parse);
        return new RequestImpl<>(this, method, parsed.resolve(baseUrl.newBuilder(), params), json);
    }

    ObjectMapper getMapper() {
        return this.mapper;
    }

    <T> CompletableFuture<T> execute(okhttp3.Request request, Function<byte[], T> decoder) {
        final var builder = request.newBuilder().header("User-Agent", USER_AGENT);
        if (request.header("Accept") == null) {
            builder.header("Accept", ACCEPT);
        }

        if (request.header("X-GitHub-Api-Version") == null) {
            builder.header("X-GitHub-Api-Version", API_VERSION);
        }

        if (this.token.isPresent()) {
            builder.header("Authorization", String.format("Bearer %s", this.token.map(String::strip).get()));
        }

        final var call = this.client.newCall(builder.build());
        final var fut = new CompletableFuture<T>();

        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                fut.completeExceptionally(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (response) {
                    final var body = response.body().bytes();
                    logRatelimit(response);

                    if (!response.isSuccessful()) {
                        fut.completeExceptionally(GitHubException.from(response, body, mapper));
                        return;
                    }

                    fut.complete(decoder.apply(body));
                }
            }
        });

        // Cancelling the future cancels the call.
        fut.whenComplete((_, _) -> {
            if (fut.isCancelled()) {
                call.cancel();
            }
        });

        return fut;
    }

    private static void logRatelimit(Response response) {
        final var remaining = response.header("x-ratelimit-remaining");
        if (remaining != null && LOG.isTraceEnabled()) {
            LOG.trace("{} {}: {}/{} requests left in this window",
                    response.request().method(),
                    response.request().url().encodedPath(),
                    remaining,
                    response.header("x-ratelimit-limit"));
        }
    }
}
