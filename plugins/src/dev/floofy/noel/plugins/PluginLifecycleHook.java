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

/// Lets a plugin react to the lifecycle of the plugins that depend on it.
///
/// This is meant to be implemented on a [Plugin] subclass. When a plugin that lists the implementing
/// plugin in [Descriptor#dependsOn()] enables or is about to be destructed, the [PluginManager] calls the
/// matching lifecycle hook with the [plugin's context][PluginContext]. Only <em>direct</em> dependents are
/// reported.
///
/// > [!NOTE]
/// > This is how infrastructure-based plugins (like `dev.floofy.noel.plugins.DiscordPlugin`) pick up contributions
/// > from other plugins without the manager knowing about them.
public interface PluginLifecycleHook {
    /// Called after a direct dependent has been enabled.
    /// @param cx dependent's plugin context
    /// @throws Exception The exception that might've been thrown to reject the dependent. Hooks that already
    ///                   ran for it are undone with [#onDependentDestructing], its [Plugin#onPluginDestruct()] is
    /// called,
    ///                   and it is treated as failed, so plugins depending on it are skipped.
    default void onDependentEnabled(PluginContext cx) throws Exception {}

    /// Called after a direct dependent is destructed, or when rolling it back. It undos everything
    /// [#onDependentEnabled(PluginContext)]
    /// did for it.
    ///
    /// @param cx dependent's plugin context
    /// @throws Exception Exception if cleanup fails. It is logged, and destruction continues.
    default void onDependentDestructing(PluginContext cx) throws Exception {}
}
