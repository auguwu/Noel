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

import tools.jackson.core.type.TypeReference;

/// A GitHub API request, built by [Client#request(Method, String, java.util.Map)].
///
/// Like a JDA action, it's a builder: add query parameters, headers or a body,
/// then send it with [#submit()] or [#complete()]. It completes
/// with the response's JSON, or a missing node for an empty response such as
/// `204 No Content`
///
/// ```java
/// github.request<JsonNode>(POST, "/repos/:owner/:repo/issues", Map.of("owner", "auguwu", "repo", "Noel"))
///         .body(Map.of("title", "Found a bug", "labels", List.of("bug")))
///         .map(issue -> issue.path("html_url").asString())
///         .queue(url -> LOG.info("opened {}", url));
/// ```
///
/// Builder methods change this request and return it. Don't change it after
/// sending it, or from several threads.
public interface Request<T> extends RestAction<T> {
    /// Adds a query parameter, such as `per_page=100`. Adding the same name
    /// twice sends it twice.
    ///
    /// @param name  the parameter's name
    /// @param value its value, converted with `String.valueOf`
    /// @param encode if the query parameter should be encoded
    /// @return this request
    Request<T> addQueryParameter(String name, Object value, boolean encode);

    /// Same as [#addQueryParameter(String, Object, boolean)] but sets `encode` to false.
    default Request<T> addQueryParameter(String name, Object value) {
        return addQueryParameter(name, value, false);
    }

    /// Adds a header, on top of the ones every request gets (`Accept`, the API
    /// version, `User-Agent` and `Authorization`). Use it for things like a
    /// different `Accept` media type.
    ///
    /// @param name  the header's name
    /// @param value its value
    /// @return this request
    Request<T> header(String name, Object value);

    /// Sets the JSON body: a map, a record, a `JsonNode`, or anything else Jackson can serialize.
    /// @param body the body
    /// @return this request
    /// @throws IllegalStateException if the method doesn't allow a body
    Request<T> body(Object body);

    /// Returns a copy of this request that deserializes the response into `type`.
    <R> Request<R> as(Class<R> type);

    /// Like [#as(Class)], for generic types such as `List<Repository>`.
    <R> Request<R> as(TypeReference<R> type);
}
