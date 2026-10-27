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

import dev.floofy.noel.birthdays.models.Birthday;
import java.util.Optional;

/// Birthdays, stored in the `birthdays` MongoDB collection.
public interface Store {
    /// Looks up a member's birthday in a guild.
    /// @param guild the guild
    /// @param user  the member
    /// @return the birthday, or empty if none is set
    Optional<Birthday> find(long guild, long user);

    /// Creates or replaces a birthday.
    /// @param birthday the birthday
    void save(Birthday birthday);

    /// Removes a member's birthday in a guild.
    /// @param guild the guild
    /// @param user  the member
    /// @return `true` if one was removed
    boolean delete(long guild, long user);
}
