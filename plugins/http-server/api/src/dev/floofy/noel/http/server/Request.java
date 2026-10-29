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

import java.net.SocketAddress;
import java.util.Optional;

/// An HTTP request.
public interface Request {
    /// Returns the requested path, it will always start with `/`
    String getPath();

    /// Returns the HTTP method.
    HttpMethod getMethod();

    /// Returns the remote connection address that established this request.
    SocketAddress getRemoteAddress();

    /// Returns a header's value. Header names are case-insensitive.
    /// @param name the header's name, such as `X-GitHub-Event`
    /// @return the first value, or empty if the header isn't present
    Optional<String> getHeader(String name);

    /// Returns a query parameter's value, decoded.
    /// @param name the parameter's name
    /// @return the first value, or empty if the parameter isn't present
    Optional<String> getQueryParameter(String name);

    /// Returns the raw body, exactly as received. Use this when the bytes
    /// themselves matter, such as for verifying a signature; re-serializing a
    /// parsed body changes them.
    ///
    /// @return a copy of the body; empty if there is none
    byte[] getBody();
}
