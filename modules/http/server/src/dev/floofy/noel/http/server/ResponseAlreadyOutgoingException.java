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

/// [Exception] that is thrown when a response is already in an outgoing state, i.e,
/// any of the `send*` methods were invoked.
public class ResponseAlreadyOutgoingException extends RuntimeException {
    public ResponseAlreadyOutgoingException() {
        super("response is already sealed and being transmitted");
    }
}
