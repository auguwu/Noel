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

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/// A base class for all plugins.
///
/// A Noel plugin is a Guice [module][com.google.inject.Module]. The plugin manager builds one child injector
/// of the core injector per plugin, with the plugin instance as one of that injector's module. Bindings declared in
/// [#configure()] and [@Provides][com.google.inject.Provides] methods belong to that injector and are private to the
/// plugin, unless they are [#export(Class)] exported.
///
/// ## Requirements
/// * Concrete subclasses must be annotated with [@Descriptor][Descriptor].
///
/// @see Descriptor
public abstract class Plugin extends AbstractModule {
    private final Set<Key<?>> exports = new LinkedHashSet<>();
    private volatile Injector injector;

    /// Returns this plugin's [descriptor annotation][Descriptor].
    public final Descriptor descriptor() {
        return getClass().getAnnotation(Descriptor.class);
    }

    /// Called once every plugin has been loaded and all of this plugin's dependencies have been enabled.
    /// This is the place to open database connections, register listeners, and start background work.
    ///
    /// @throws Exception if the plugin cannot start. the plugin is marked as failed and every plugin that
    ///                   depends on this plugin is skipped.
    public void onPluginEnable() throws Exception {}

    /// Called once, after every plugin has been through the enable pass, on each plugin that enabled
    /// successfully.
    ///
    /// Use this for work that needs to see the final set of enabled plugins.
    /// @throws Exception if the work fails. It is logged; the plugin stays enabled.
    public void onPluginsReady() throws Exception {}

    /// Called when this plugin is being shut down or unloaded. Only called if [#onPluginEnable()] completed
    /// successfully.
    ///
    /// Plugins are destructed in reverse dependency order, so every plugin that
    /// depends on this one has already been destructed, while this plugin's own
    /// dependencies are still available. This plugin's slash commands have already
    /// been unregistered. Release everything acquired in [#onPluginEnable()]:
    /// listeners, connections, executors and scheduled tasks. Anything left running
    /// may keep the plugin's classes reachable after unload.
    public void onPluginDestruct() throws Exception {}

    /// Returns this plugin's final [injector][Injector], which can see core bindings, dependencies' exports,
    /// and this plugin's own bindings.
    ///
    /// This is intended to be used in lifecycle hooks that need the plugin's own bindings, which cannot be injected
    /// directly into the plugin directly. Prefer constructor injection and [@Provides][com.google.inject.Provides]
    /// parameters wherever they work.
    ///
    /// @throws IllegalStateException if called before the final injector has been created.
    protected final Injector injector() throws IllegalStateException {
        if (this.injector == null) {
            throw new IllegalStateException("`injector()` is only available after configuration, from lifecycle hooks");
        }

        return this.injector;
    }

    /// Exports a binding so that plugins depending on this one can inject it.
    ///
    /// This must be called from [#configure()], and the type must also be
    /// bound in this plugin's injector, whether by {@code configure()}, a
    /// [@Provides][com.google.inject.Provides] method, or an installed module.
    /// Bindings that are not exported stay private to this plugin.
    ///
    /// @param type the bound type to export; use {@link #export(Key)} for
    ///             qualified or generic types
    /// @param <T>  the bound type
    protected final <T> void export(Class<T> type) {
        this.exports.add(Key.get(type));
    }

    /// Exports a binding by key, so that plugins depending on this one can inject it.
    ///
    /// Use this overload for qualified or generic bindings, for example
    /// `export(Key.get(new TypeLiteral<Cache<String, Guild>>() {}))` or
    /// `export(Key.get(HttpClient.class, Names.named("discord")))`.
    ///
    /// This must be called from [#configure()], and the key must be bound
    /// in this plugin's injector.
    ///
    /// @param key the binding key to export
    /// @param <T> the bound type
    protected final <T> void export(Key<T> key) {
        this.exports.add(key);
    }

    /// Returns the keys this plugin has exported.
    ///
    /// The set is only complete after [#configure()] has run, which is
    /// after the final injector has been created.
    ///
    /// @return an unmodifiable view of the exported keys, in declaration order
    final Set<Key<?>> exports() {
        return Collections.unmodifiableSet(this.exports);
    }

    /// Attaches the final injector. Called once by the plugin manager immediately
    /// after the final injector is created.
    ///
    /// @param injector the plugin's final injector
    /// @throws IllegalStateException if an injector is already attached
    final void attach(Injector injector) {
        if (this.injector != null) {
            throw new IllegalStateException(String.format(
                    "plugin [%s] is already attached its injector; reloading requires a new instance",
                    this.describe()));
        }

        this.injector = injector;
    }

    private String describe() {
        var desc = this.descriptor();
        return desc != null ? desc.id() : getClass().getName();
    }

    @Override
    public final boolean equals(Object that) {
        return this == that;
    }

    @Override
    public final int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "NoelPlugin[" + describe() + "]";
    }
}
