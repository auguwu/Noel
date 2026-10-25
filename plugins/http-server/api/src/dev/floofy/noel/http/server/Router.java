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

/// Maps an HTTP method and an exact path to the handler that serves it.
///
/// Routes can be added and removed while the server is running, from any
/// thread. Each route method returns a [Registration]; close it to remove the
/// route, typically in your plugin's `onPluginDestruct`.
///
/// ## Handlers
/// Handlers run on Netty's I/O threads, so they must not block. A handler that
/// waits on a database or another HTTP call stalls every connection on that
/// thread; hand that work to an executor and send the response from there.
///
/// A handler that throws gets a `500` reply, if it hasn't responded yet, and
/// the exception is logged.
public interface Router {
    /// Routes requests with `method` to exactly `path`.
    /// @param method  the HTTP method
    /// @param path    the exact path, such as `/webhooks/github`
    /// @param handler serves the request
    /// @return a registration that removes the route when closed
    /// @throws IllegalStateException if `method` and `path` are already routed
    Registration route(HttpMethod method, String path, BiConsumer<Request, Response> handler);

    /// Routes `GET` requests to `path`.
    /// @param path    the exact path
    /// @param handler serves the request
    /// @return a registration that removes the route when closed
    /// @throws IllegalStateException if `GET path` is already routed
    default Registration get(String path, BiConsumer<Request, Response> handler) {
        return route(HttpMethod.Get, path, handler);
    }

    /// Routes `POST` requests to `path`.
    /// @param path    the exact path
    /// @param handler serves the request
    /// @return a registration that removes the route when closed
    /// @throws IllegalStateException if `POST path` is already routed
    default Registration post(String path, BiConsumer<Request, Response> handler) {
        return route(HttpMethod.Post, path, handler);
    }

    /// A route that was added. Closing it removes the route; closing it again
    /// does nothing.
    @FunctionalInterface
    interface Registration extends AutoCloseable {
        /// Removes the route. Requests already being handled finish normally.
        @Override
        void close();
    }
}
