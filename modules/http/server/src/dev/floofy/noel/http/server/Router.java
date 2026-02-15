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

package dev.floofy.noel.http.server;

import java.util.function.BiConsumer;

/// A simple router that is used to route requests when an HTTP connection is incoming
public interface Router {
    /// Route a path with a specified HTTP method.
    void route(HttpMethod method, String path, BiConsumer<Request, Response> consumer);

    /// Route a GET request in the specified `path`.
    default void get(String path, BiConsumer<Request, Response> consumer) {
        route(HttpMethod.GET, path, consumer);
    }

    /// Route a POST request in the specified `path`.
    default void post(String path, BiConsumer<Request, Response> consumer) {
        route(HttpMethod.POST, path, consumer);
    }
}
