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

import java.util.Set;

/// Decides which guilds a slash command is registered in, from the owning
/// plugin's own state: its configuration, its database, anything.
///
/// Attach one to a command with `@GuildRegistration`. Pinecone builds
/// it with the owning plugin's injector, so it can inject the plugin's bindings,
/// and asks it on every sync:
///
/// ```java
/// /// Servers that turned birthdays on.
/// final class BirthdayGuilds implements GuildScope {
///     private final Settings settings;
///
///     @Inject
///     BirthdayGuilds(Settings settings) {
///         this.settings = settings;
///     }
///
///     @Override
///     public Set<Long> guilds() {
///         return BirthdayConfig.guilds(settings);
///     }
/// }
///
/// @SlashCommand(name = "birthday", description = "...")
/// @GuildRegistration(BirthdayGuilds.class)
/// public final class BirthdayCommand extends AbstractSlashCommand { ... }
/// ```
///
/// ## When the answer changes
/// Discord only learns about a change when Pinecone syncs. A sync already
/// happens on every configuration reload, so a scope that reads the
/// configuration needs nothing more. A scope backed by anything else, like a
/// database that a command writes to, must call `Pinecone#resync()` after the
/// change.
///
/// ## Precedence
/// `commands.<name>.onlyInGuilds` in the configuration still wins, so a scoped
/// command can be pinned somewhere by hand. A command can't have both a scope
/// and [dev.floofy.noel.pinecone.annotations.SlashCommand#onlyInGuilds()].
public interface GuildScope {
    /// Returns the guilds the command is registered in.
    ///
    /// An empty set registers the command nowhere: it disappears from every
    /// guild's command list. There is no way to return "global"; a command that
    /// can be global shouldn't have a scope.
    ///
    /// Called during every sync, on Pinecone's sync thread, so it may block
    /// briefly, but should be quick. If it throws, the command is registered
    /// nowhere until the next sync, and the error is logged: hiding a command
    /// is safer than showing it where it isn't allowed.
    ///
    /// @return the guild IDs; guilds Noel isn't in are skipped with a warning
    Set<Long> guilds();
}
