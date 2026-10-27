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

package dev.floofy.noel.birthdays.api;

/// Announces birthdays as their day starts.
///
/// Rather than a timer per member, the scheduler polls once a minute for
/// birthdays whose `nextAt` has passed. All state is in MongoDB, so restarts and
/// redeploys lose nothing: a birthday whose day began while the bot was down is
/// still announced, as long as that day hasn't ended in the member's timezone.
///
/// Each birthday is claimed (moved on to next year) *before* it is announced, so
/// it is announced at most once. If Discord rejects the message, that year's
/// announcement is lost rather than repeated every minute.
///
/// ## Starting and stopping
/// [#start()] and [#close()] can be called any number of times, in turn, so the
/// plugin can be disabled and re-enabled. [#close()] waits for a tick that's
/// already running, so nothing touches MongoDB or Discord after it returns.
public interface Scheduler extends AutoCloseable {
    /// Starts polling once a minute, beginning now.
    /// @throws IllegalStateException if it's already running
    void start() throws Exception;

    /// Stops polling, and waits up to 10 seconds for a running tick to finish.
    /// Does nothing if it isn't running.
    ///
    /// The executor is shared with the rest of the bot, so only this
    /// scheduler's task is canceled, never the executor.
    @Override
    void close() throws Exception;
}
