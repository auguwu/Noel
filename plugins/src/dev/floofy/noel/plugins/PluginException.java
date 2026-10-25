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

package dev.floofy.noel.plugins;

/// Thrown when a plugin cannot be discovered, resolved, or loaded.
///
/// The [plugin manager][PluginManager] throws this when:
/// * a plugin class is missing its [@Descriptor][Descriptor], or has a blank ID
/// * two plugins share the same ID
/// * a plugin depends on itself, on a class that is not on the classpath, or on a plugin that is
/// not loaded.
/// * the dependency graph contains a lifecycle.
/// * Guice fails to construct or configure a plugin
///
/// Loading is all-or-nothing, so when this is thrown from
/// [PluginManager#loadAll(ClassLoader)], no plugin remains loaded. The message
/// names the plugin involved.
public final class PluginException extends RuntimeException {
    public final String id;

    /// Creates an exception with the given message.
    ///
    /// @param id plugin id.
    /// @param message a description of the failure, naming the plugin involved
    public PluginException(String id, String message) {
        super(String.format("%s %s", id != null ? String.format("[plugin %s]", id) : "", message));

        this.id = id;
    }

    /// Creates an exception with the given message and cause.
    ///
    /// @param id plugin id.
    /// @param message a description of the failure, naming the plugin involved
    /// @param cause   the underlying failure, such as a Guice {@code CreationException}
    ///                or a {@link TypeNotPresentException}
    public PluginException(String id, String message, Throwable cause) {
        super(String.format("%s %s", id != null ? String.format("[plugin %s]", id) : "", message), cause);

        this.id = id;
    }
}
