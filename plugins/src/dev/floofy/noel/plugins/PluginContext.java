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

import com.google.inject.Injector;

/// A loaded plugin, as reported to a [PluginLifecycleHook].
/// @param descriptor the plugin's {@link Descriptor}
/// @param type       the plugin's class; its class loader and package identify the
///                   plugin's own code, for example when scanning for contributions
/// @param instance   the plugin instance
/// @param injector   the plugin's final injector, which can see core bindings, its
///                   dependencies' exports, and its own bindings. Hooks that need
///                   to construct the plugin's classes with injection should create
///                   a child of this injector rather than adding to it.
public record PluginContext(Descriptor descriptor, Class<? extends Plugin> type, Plugin instance, Injector injector) {}
