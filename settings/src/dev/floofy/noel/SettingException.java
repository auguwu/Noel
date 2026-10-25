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

package dev.floofy.noel;

import dev.floofy.noel.settings.Key;

/// Thrown when a [Setting] is missing where it is required, or present with a
/// value that cannot be read as the setting's type.
///
/// The message always starts with the setting's path, for example
/// `setting [discord.owners[2]]: expected a string, got 5 (Integer)`.
public final class SettingException extends RuntimeException {
    private final Key key;

    /// Creates an exception for a setting.
    /// @param key     the setting's key
    /// @param message what is wrong with it
    public SettingException(Key key, String message) {
        super(format(key, message));
        this.key = key;
    }

    /// Creates an exception for a setting, with a cause.
    /// @param key     the setting's key
    /// @param message what is wrong with it
    /// @param cause   the underlying failure
    public SettingException(Key key, String message, Throwable cause) {
        super(format(key, message), cause);
        this.key = key;
    }

    static SettingException invalid(Key key, String expected, Object actual) {
        return new SettingException(key, String.format("expected [%s], got [%s]", expected, Setting.describe(actual)));
    }

    /// Returns the key of the setting that failed.
    public Key key() {
        return key;
    }

    private static String format(Key key, String message) {
        var separator = message.startsWith("is ") ? " " : ": ";
        return "setting [" + key.path() + "]" + separator + message;
    }
}
