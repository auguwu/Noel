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

package dev.floofy.noel.github.webhooks.api;

import dev.floofy.noel.http.server.Request;
import dev.floofy.noel.http.server.Response;

/// Receives GitHub webhook deliveries at `POST /webhooks/github?id=<webhook ID>`
/// and posts them to the webhook's channel.
///
/// ## Handling a delivery
/// 1. On Netty's thread: read the ID, headers and body, then hand off. Looking
///    the webhook up is a MongoDB query, which mustn't block Netty, and the
///    request's buffer is gone once the handler returns.
///
/// 2. Look the webhook up. Unknown ID: `404`.
/// 3. Check `X-Hub-Signature-256` against its secret. Wrong or missing: `401`,
///    and nothing is posted.
/// 4. `ping` (sent when the webhook is created in GitHub) posts a "connected"
///    message: `200`.
/// 5. Anything filtered out (another repository, an event not chosen, an action
///    that isn't posted, like an issue being labelled): `202`, nothing posted.
///
/// 6. Otherwise, the message is queued to Discord: `202`.
///
/// GitHub shows these statuses and bodies under the webhook's "Recent
/// Deliveries", which makes them the easiest place to debug a webhook from.
/// GitHub gives up after 10 seconds; the reply is sent before posting to
/// Discord, so a slow Discord doesn't count against that.
@FunctionalInterface
public interface Receiver {
    /// Handles the HTTP request.
    void handle(Request req, Response res);
}
