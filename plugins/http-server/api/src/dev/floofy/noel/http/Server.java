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

package dev.floofy.noel.http;

import dev.floofy.noel.http.server.Router;

/// Noel's HTTP server.
///
/// Exported by `HttpServerPlugin`, which starts and stops it. Plugins that depend
/// on it only need [#getRouter()].
public interface Server {
    /// Returns the number of requests currently being handled.
    /// Requests are handled on Netty's threads, so this never blocks.
    int getInflightRequests();

    /// Returns the router that decides which handler serves a request.
    ///
    /// Routes can be added and removed at any time, including while the server
    /// is running.
    Router getRouter();

    /// Binds the configured address and port, then returns. Connections are
    /// accepted on Netty's threads.
    ///
    /// A server that was [shut down][#shutdown()] can be started again; the
    /// address and port are re-read from the configuration each time.
    ///
    /// @throws IllegalStateException if the server is already running
    /// @throws Exception if the address can't be bound
    void start() throws Exception;

    /// Stops accepting connections, closes open ones, and waits for Netty's
    /// threads to stop. Does nothing if the server isn't running.
    ///
    /// @throws Exception if closing the listening socket fails
    void shutdown() throws Exception;

}
