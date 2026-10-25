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

import java.lang.annotation.*;

/// Declares a class as a [Plugin] and describes how its fits into the load order.
///
/// > [!NOTE]
/// > In the previous iteration of this plugin system (called "modules"), the priority was
/// > explicitly set in the descriptor itself. The new plugins system now can carry dependency
/// > information.
///
/// Every concrete [Plugin] must carry this annotation. The plugin manager reads this descriptor
/// at discovery time to identify the plugin, build the dependency graph, and decide what order
/// plugins are configured and enabled in.
///
/// ## Example Plugin
/// ```java
/// @Descriptor(
///     id = "dev.floofy.noel.plugins.HttpServer",
///     description = "A embedded HTTP server and router",
///     dependsOn = {SettingsPlugin.class}
/// )
/// public final class HttpServerPlugin extends Plugin {
///     @Inject
///     private Settings settings;
///
///     @Override
///     protected void configure() {
///         /* ... */
///     }
/// }
/// ```
///
/// @see Plugin
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Descriptor {
    /// Unique identifier for this plugin.
    ///
    /// The `id` must be unique across every plugin loaded by the same manager; a duplicate
    /// ID fails loading. By convention, it is a reserve-DNS name, such as `dev.floofy.noel.plugins.HttpServerPlugin`.
    /// It
    /// is used in diagnostics and logs, and it should stay stable across releases, since tooling may refer to the
    /// plugin by it.
    ///
    /// @return unique identifier; never empty.
    String id();

    /// A short, human-readable summary of what the plugin does.
    String description() default "<unknown description>";

    /// A list of dependencies this plugin requires and depends on. They are configured and enabled
    /// before this one.
    ///
    /// Each listed dependency:
    /// * must itself be annotated with [Descriptor] and be present at load time. A missing dependency,
    ///   including one whose class cannot be resolved (a [TypeNotPresentException] when this attribute
    ///   is read), fails loading for this plugin;
    /// * has its [onPluginEnable][Plugin#onPluginEnable()] called before this plugin's, and its
    ///   [onPluginDestruct][Plugin#onPluginDestruct()] called after this plugin's;
    /// * has its exported bindings, and its own plugin instance, made available for injection into this plugin.
    ///   Bindings it does not export stay private to its injector.
    ///
    /// Dependencies are not transitive for injection. If this plugin needs bindings from a dependency's dependency,
    /// list that plugin here as well. Cycles anywhere in the graph are rejected when load order is resolved.
    Class<? extends Plugin>[] dependsOn() default {};
}
