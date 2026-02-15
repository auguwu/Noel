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

import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.IOException;

/// A HTTP response.
public interface Response {
    /// Sets the response status code.
    /// @throws ResponseAlreadyOutgoingException if this response is already sealed and being transmitted
    Response setStatus(HttpResponseStatus status) throws ResponseAlreadyOutgoingException;

    /// @throws ResponseAlreadyOutgoingException if this response is already sealed and being transmitted
    Response setHeader(String name, Object value) throws ResponseAlreadyOutgoingException;

    /// @throws ResponseAlreadyOutgoingException if this response is already sealed and being transmitted
    void sendText(String text) throws ResponseAlreadyOutgoingException;
    void sendJSON(Object obj) throws IOException;
}
