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

package dev.floofy.noel.pinecone.api;

import com.google.inject.Injector;
import com.google.inject.Provider;
import dev.floofy.noel.pinecone.annotations.SlashCommand;
import java.util.List;
import java.util.Optional;

/// Every slash command contributed by a plugin, keyed by command name.
///
/// Registration only changes this registry. Pushing commands to Discord is
/// the implementation's job: once after every plugin has enabled, and again on each configuration reload.
///
/// Every command is validated as it registers, so mistakes
/// like a missing description or a misnamed `@Autocomplete` fail the owning
/// plugin, by name, instead of breaking the sync for every command.
///
/// ## Instances
///
/// Command classes are bound **unscoped**, and every invocation gets a fresh
/// instance from [Registered#provider()]. Pinecone resolves `@Option` fields
/// into the command instance, so a shared instance would let two concurrent
/// invocations overwrite each other's options. One [Registered#prototype()]
/// instance per command is kept for metadata: its name, options and subcommands.
public interface CommandRegistry {
    /// A registered slash command.
    /// @param pluginId  the id of the plugin that owns it
    /// @param info      its `@SlashCommand` annotation
    /// @param prototype an instance used only for metadata; never executed
    /// @param provider  creates the instance each invocation runs on
    /// @param scope     decides which guilds it's registered in, from
    ///                  [`@GuildRegistration`][dev.floofy.noel.pinecone.annotations.GuildRegistration]; `null` if
    ///                  it has none
    record Registered(
            String pluginId,
            SlashCommand info,
            AbstractSlashCommand prototype,
            Provider<? extends AbstractSlashCommand> provider,
            List<? extends CommandInterceptor> interceptors,
            GuildScope scope) {}

    /// Scans `owner`'s package for slash commands and registers them for a plugin.
    ///
    /// The commands are bound in a child of `parent`, so they can inject core
    /// bindings, the plugin's own bindings, and its dependencies' exports.
    /// Registration is all-or-nothing for the plugin.
    ///
    /// @param pluginId the owning plugin's id
    /// @param parent   the owning plugin's final injector
    /// @param owner    the owning plugin's class, which bounds the scan
    /// @return the number of commands registered
    /// @throws IllegalStateException if a command is invalid, or its name is already registered by any plugin
    int register(String pluginId, Injector parent, Class<?> owner);

    /// Removes every command a plugin registered.
    /// @param pluginId the owning plugin's id
    void unregisterAll(String pluginId);

    /// Looks up a command by name.
    /// @param name the slash command name, without the `/`
    /// @return the command, or empty if none is registered
    Optional<Registered> find(String name);

    /// Returns every registered command.
    /// @return a snapshot of the registered commands
    List<Registered> all();
}
