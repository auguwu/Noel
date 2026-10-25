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

import tools.jackson.databind.JsonNode;

import java.util.Map;

/// A client for GitHub's REST API, in the style of JDA.
///
/// [#request(Method, String, Map)] builds a [Request] from a route template, and nothing is sent until it's queued:
///
/// ```java
/// import static dev.floofy.noel.github.Method.*;
///
/// github.request(GET, "/users/:username", Map.of("username", "auguwu"))
///         .queue(user -> LOG.info("{} has {} followers",
///                 user.path("login").asString(), user.path("followers").asInt()));
///
/// // the authenticated user; GitHub has no `/users/me`
/// github.request(GET, "/user").queue(...);
/// ```
///
/// Every request is sent with the headers GitHub asks for: `Accept:
/// application/vnd.github+json`, `X-GitHub-Api-Version`, a `User-Agent`, and
/// the token, if one is configured.
///
/// ## Errors
/// An error status fails the action with a [GitHubException], which says
/// whether it's a rate limit and how long to wait. Nothing is retried
/// automatically.
///
/// ## Threads
/// Requests run on the `OkHttpClient`'s dispatcher, so callbacks run on its
/// threads. The client is shared with the rest of the bot, through the HTTP
/// client plugin.
public interface Client {
    /// Builds a request, filling the route's `:name` and `*name` parameters from
    /// `parameters`:
    /// ```java
    /// github.request(GET, "/repos/:owner/:repo/contents/*path",
    ///         Map.of("owner", "auguwu", "repo", "Noel", "path", "bot/BUILD.bazel"));
    /// ```
    ///
    /// Mistakes in the route or the parameters throw here, when the request is
    /// built, rather than failing the request later.
    ///
    /// @param method     the HTTP method
    /// @param route      the route template
    /// @param params     a value for every parameter in the route, and nothing else
    /// @return the request, not yet sent
    /// @throws IllegalArgumentException if the route is malformed, a parameter
    ///         is missing, or `parameters` has a name the route doesn't use
    Request<JsonNode> request(Method method, String route, Map<String, ?> params);

    default Request<JsonNode> request(Method method, String path) {
        return request(method, path, Map.of());
    }
}
