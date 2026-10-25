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

package dev.floofy.noel.github.api;

import okhttp3.Response;
import tools.jackson.databind.ObjectMapper;

import java.io.Serial;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/// GitHub answered a request with an error status.
///
/// The message is GitHub's own, such as `Not Found` or `Bad credentials`,
/// prefixed with the request: `GET /users/nobody: 404 Not Found`.
public final class GitHubException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean wasRatelimitExhausted;
    private final String documentationUrl;
    private final Instant ratelimitReset;
    private final Duration retryAfter;
    private final String method;
    private final String path;
    private final int status;

    private GitHubException(
            String message,
            int status,
            String method,
            String path,
            String documentationUrl,
            Duration retryAfter,
            Instant ratelimitReset,
            boolean wasRatelimitExhausted
    ) {
        super(message);

        this.wasRatelimitExhausted = wasRatelimitExhausted;
        this.documentationUrl = documentationUrl;
        this.ratelimitReset = ratelimitReset;
        this.retryAfter = retryAfter;
        this.method = Objects.requireNonNull(method, "method");
        this.status = status;
        this.path = Objects.requireNonNull(path, "path");
    }

    public static GitHubException from(Response response, byte[] body, ObjectMapper mapper) {
        final var request = response.request();
        var message = response.message();
        String documentationUrl = null;

        try {
            final var node = mapper.readTree(body);
            if (node.path("message").isString()) {
                message = node.path("message").asString();
            }

            if (node.path("documentationUrl").isString()) {
                documentationUrl = node.path("documentationUrl").asString();
            }
        } catch (RuntimeException _) {
            // not JSON, which happens sometimes! we can just ignore it.
        }

        final var retryAfter = parseLong(response.header("retry-after")).map(Duration::ofSeconds).orElse(null);
        final var reset = parseLong(response.header("x-ratelimit-reset")).map(Instant::ofEpochSecond).orElse(null);
        final var exhausted = "0".equals(response.header("x-ratelimit-remaining"));

        return new GitHubException(
                String.format("[%s %s] %d: %s", request.method(), request.url().encodedPath(), response.code(), message),
                response.code(),
                request.method(),
                request.url().encodedPath(),
                documentationUrl,
                retryAfter,
                reset,
                exhausted);
    }

    /// Returns the HTTP status, such as `404`.
    public int getStatus() {
        return status;
    }

    /// Returns the request's method, such as `GET`.
    public String getMethod() {
        return method;
    }

    /// Returns the request's path, such as `/users/nobody`.
    public String getPath() {
        return path;
    }

    /// Returns the docs link GitHub included with the error, if any.
    public Optional<String> getDocumentationUrl() {
        return Optional.ofNullable(documentationUrl);
    }

    /// Whether this is a rate limit rather than a real error.
    ///
    /// GitHub answers `429`, or `403` with either a `Retry-After` header (a
    /// secondary rate limit) or no requests left in the hour (the primary one).
    /// A plain `403` is a permissions problem and doesn't count.
    ///
    /// @return whether to retry later
    public boolean isRateLimited() {
        return status == 429 || (status == 403 && (retryAfter != null || wasRatelimitExhausted));
    }

    /// Returns how long to wait before retrying, for a rate-limited request:
    /// `Retry-After` if GitHub sent it, otherwise the time until the hourly
    /// limit resets.
    ///
    /// @return how long to wait, or empty if this isn't a rate limit
    public Optional<Duration> getRetryAfter() {
        if (!isRateLimited()) {
            return Optional.empty();
        }

        if (retryAfter != null) {
            return Optional.of(retryAfter);
        }

        if (ratelimitReset != null) {
            final var wait = Duration.between(Instant.now(), ratelimitReset);
            return Optional.of(wait.isNegative() ? Duration.ZERO : wait);
        }

        // GitHub's advice when it gives neither: wait at least a minute.
        return Optional.of(Duration.ofMinutes(1));
    }

    private static Optional<Long> parseLong(String header) {
        if (header == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(Long.parseLong(header.strip()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
