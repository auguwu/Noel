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

import com.google.inject.*;
import io.github.classgraph.ClassGraph;
import java.io.Closeable;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Discovers, loads, enables and destructs [Plugin]s.
///
/// ## Phases
/// A manager moves through its phases exactly once:
/// * [ClassLoader] discovers plugins, resolves their load order via [Descriptor#dependsOn()], and builds
///   every plugin's injectors. No plugin has side effects (yet). If anything fails, nothing is loaded and the
///   whole call fails.
/// * [#enableAll()] enables plugins in dependency order. For each plugin, [Plugin#onPluginEnable()] is called, then
/// every
///   direct dependency that implements [PluginLifecycleHook] is notified. A plugin that fails is skipped along with
/// everything
///   that depends on it; the rest still enable. Once the pass is done, [Plugin#onPluginsReady()] is called on every
/// enabled plugin.
/// * [#destroyAll()] (or [#close()]) destructs enabled plugins in reverse order: hooks are notified first, then
/// [Plugin#onPluginDestruct()]
///   is called.
///
/// ## Guice Injectors
/// Every plugin gets two child injectors of the core injector:
///
/// * `pre`   - dependency exports, the plugin's [@Descriptor][Descriptor], the plugin class.
/// * `final` - the plugin as a module
///
/// The plugin is constructed by `pre`, so its constructor and [@Inject][com.google.inject.Inject] members can use
/// core bindings and its dependencies' exports. Its own bindings live in `final`. Both injectors require explicit
/// bindings,
/// which keeps JIT (just-in-time) bindings for plugin classes from being created in the core injector.
///
/// ## Remarks
/// Dependency Injection is not transitive: a plugin sees the exports of the plugins it lists in
/// [Descriptor#dependsOn()], plus
/// those plugin instances themselves, and nothing from their dependencies.
///
/// ## Discovery
/// Plugins are found with [java.util.ServiceLoader], so each plugin JAR needs a `META-INF/services/` entry for
/// [Plugin]. Anything
/// plugins contribute to each other is discovered by the plugin that owns the feature, through [PluginLifecycleHook].
@Singleton
public final class PluginManager implements Closeable {
    /// A plugin that has been constructed and configured.
    private record LoadedPlugin(
            Class<? extends Plugin> type,
            Plugin instance,
            Descriptor descriptor,
            Injector injector,
            Set<Key<?>> exports,
            List<PluginLifecycleHook> hooks) {
        /// Creates a new [PluginContext] for this loaded plugin.
        public PluginContext context() {
            return new PluginContext(this.descriptor, this.type, this.instance, this.injector);
        }
    }

    private enum State {
        Uninit,
        Loaded,
        Enabled,
        Destroyed;
    }

    private static final Logger LOG = LoggerFactory.getLogger(PluginManager.class);

    private final Map<Class<? extends Plugin>, LoadedPlugin> loadedPlugins = new LinkedHashMap<>();
    private final List<LoadedPlugin> enabledPlugins = new ArrayList<>();
    private final Injector coreInjector;

    private State state = State.Uninit;

    /// Creates a new plugin manager whose plugins are child injectors of `core`.
    /// @param core the core injector; every plugin can inject its bindings
    @Inject
    public PluginManager(Injector core) {
        this.coreInjector = Objects.requireNonNull(core, "core injector passed in was `null`");
    }

    /// Discovers and loads every plugin visible to `loader`.
    ///
    /// Plugins are constructed and configured in dependency order. No plugin's
    /// [Plugin#onPluginEnable()] is called; see [#enableAll()].
    ///
    /// This is all-or-nothing. If any plugin fails validation or fails to
    /// construct or configure, nothing remains loaded and the manager can be
    /// loaded again.
    ///
    /// @param loader the class loader to discover plugins from
    /// @return the descriptors of the loaded plugins, in load order
    /// @throws PluginException if a plugin is missing its {@link Descriptor}, two
    ///         plugins share an id, a dependency is missing, the dependency graph
    ///         has a cycle, an export is not bound, or Guice fails to build a
    ///         plugin's injectors
    /// @throws IllegalStateException if plugins have already been loaded
    public synchronized List<Descriptor> loadAll(ClassLoader loader, String... packages) {
        return loadAll(discover(loader, packages));
    }

    /// Finds concrete classes annotated with [Descriptor] that are visible to
    /// `loader`, loaded through it.
    ///
    /// @param loader   the class loader to scan and load through
    /// @param packages packages to limit the scan to; everything if empty
    /// @return the plugin classes, in no particular order
    /// @throws PluginException if an annotated class cannot be loaded or does not extend [Plugin]
    static List<Class<? extends Plugin>> discover(ClassLoader loader, String... packages) {
        var graph =
                new ClassGraph().overrideClassLoaders(loader).enableClassInfo().enableAnnotationInfo();

        if (packages.length > 0) {
            graph.acceptPackages(packages);
        }

        try (var scan = graph.scan()) {
            var out = new ArrayList<Class<? extends Plugin>>();
            for (var info : scan.getClassesWithAnnotation(Descriptor.class)) {
                if (info.isAbstract() || info.isInterface()) {
                    continue;
                }

                Class<?> cls;
                try {
                    cls = Class.forName(info.getName(), /*initialize=*/ false, loader);
                } catch (ClassNotFoundException | LinkageError ex) {
                    throw new PluginException(
                            null, String.format("failed to load plugin class [%s]", info.getName()), ex);
                }

                if (!Plugin.class.isAssignableFrom(cls)) {
                    throw new PluginException(
                            null,
                            String.format(
                                    "plugin class [%s] is annotated with `@Descriptor` but does not extend Plugin",
                                    info.getName()));
                }

                out.add(cls.asSubclass(Plugin.class));
            }

            return out;
        }
    }

    /// Loads the given plugin classes, skipping discovery. Behaves exactly like
    /// [#loadAll(ClassLoader, String...)] otherwise.
    ///
    /// Package-private so tests can load an exact set of fixture plugins.
    ///
    /// @param types the plugin classes to load, in any order
    /// @return the descriptors of the loaded plugins, in load order
    /// @throws PluginException under the same conditions as [#loadAll(ClassLoader, String...)]
    /// @throws IllegalStateException if plugins have already been loaded
    synchronized List<Descriptor> loadAll(List<Class<? extends Plugin>> types) {
        requireState(State.Uninit, "loadAll");

        try {
            for (var type : resolveOrder(types)) {
                loadedPlugins.put(type, load(type));
            }
        } catch (RuntimeException e) {
            loadedPlugins.clear();
            throw e;
        }

        state = State.Loaded;
        LOG.info("Loaded {} plugin(s): {}", loadedPlugins.size(), ids(loadedPlugins.values()));

        return mkDescriptorsFor(loadedPlugins.values());
    }

    /// Enables every loaded plugin in dependency order, then signals that all plugins are ready.
    ///
    /// For each plugin:
    /// * It is skipped if any plugin in its {@link Descriptor#dependsOn()} failed or was skipped.
    /// * {@link Plugin#onPluginEnable()} is called.
    /// * Each direct dependency implementing {@link PluginLifecycleHook} is
    ///   notified through {@link PluginLifecycleHook#onDependentEnabled}, in
    ///   {@code dependsOn} order. If one throws, the hooks that already ran are
    ///   undone in reverse, {@link Plugin#onPluginDestruct()} is called, and the
    ///   plugin counts as failed.
    ///
    /// After every plugin has been processed, [Plugin#onPluginsReady()] is
    /// called on each enabled plugin, in enable order. A failure there is logged but
    /// does not disable the plugin.
    ///
    /// @return the descriptors of the plugins that enabled successfully, in enable order
    /// @throws IllegalStateException if plugins have not been loaded, or have
    ///         already been enabled
    public synchronized List<Descriptor> enableAll() {
        requireState(State.Loaded, "enableAll");

        var failed = new HashSet<Class<? extends Plugin>>();
        for (var lp : loadedPlugins.values()) {
            var id = lp.descriptor().id();
            var hasBrokenDependency = Arrays.stream(lp.descriptor().dependsOn())
                    .filter(failed::contains)
                    .findFirst();

            if (hasBrokenDependency.isPresent()) {
                LOG.warn("skipping plugin [{}]: dependency {} did not enable", id, idOf(hasBrokenDependency.get()));
                failed.add(lp.type());

                continue;
            }

            try {
                lp.instance().onPluginEnable();
            } catch (Exception ex) {
                LOG.error("failed to enable plugin [{}]:", id, ex);
                failed.add(lp.type());

                continue;
            }

            if (!notifyEnabled(lp)) {
                failed.add(lp.type());
                continue;
            }

            enabledPlugins.add(lp);
            LOG.info("enabled plugin [{}]", id);
        }

        this.state = State.Enabled;
        if (!failed.isEmpty()) {
            LOG.warn(
                    "{} plugin(s) did not enable: {}",
                    failed.size(),
                    failed.stream().map(PluginManager::idOf).sorted().toList());
        }

        for (var lp : enabledPlugins) {
            try {
                lp.instance().onPluginsReady();
            } catch (Exception ex) {
                LOG.error(
                        "plugin [{}] failed in `onPluginsReady()`:",
                        lp.descriptor().id(),
                        ex);
            }
        }

        return mkDescriptorsFor(enabledPlugins);
    }

    /// Destructs every enabled plugin in reverse enable order.
    ///
    /// For each plugin, its direct dependencies implementing [PluginLifecycleHook] are notified through
    /// [PluginLifecycleHook#onDependentDestructing] in reverse `dependsOn` order, then [Plugin#onPluginDestruct()]
    /// is called. Failures are logged and do not stop the remaining plugins from being destructed. Plugins that never
    /// enabled are not destructed.
    ///
    /// After this call the manager holds no plugins and cannot be reused. Calling it again has no effect.
    public synchronized void destroyAll() {
        if (this.state == State.Destroyed) {
            return;
        }

        for (var i = enabledPlugins.size() - 1; i >= 0; i--) {
            var lp = enabledPlugins.get(i);
            notifyDestructing(lp, lp.hooks());
            destructPlugin(lp);
        }

        enabledPlugins.clear();
        loadedPlugins.clear();

        this.state = State.Destroyed;
    }
    /// Disables a plugin and every enabled plugin that depends on it, directly or
    /// not. Dependents are destructed first, exactly like [#destroyAll()] does.
    ///
    /// @param id   the plugin to disable
    /// @param keep ids that must stay enabled; if the cascade would reach one of
    ///             them, nothing is disabled
    /// @return the plugins that were disabled, in the order they were destructed
    /// @throws PluginException if no plugin has that id, it isn't enabled, or the
    ///         cascade would disable a plugin in `keep`
    public synchronized List<Descriptor> disable(String id, Set<String> keep) {
        requireState(State.Enabled, "disable");

        var root = find(id);
        if (!enabledPlugins.contains(root)) {
            throw new PluginException(id, "plugin is not enabled");
        }

        // doomfist was here -- totally legit
        // noel shut the fuck up and stop being overwatch pilled
        var doomed = new HashSet<Class<? extends Plugin>>();
        var victims = new ArrayList<LoadedPlugin>();
        for (var lp: enabledPlugins) {
            if (lp == root || Arrays.stream(lp.descriptor().dependsOn()).anyMatch(doomed::contains)) {
                doomed.add(lp.type());
                victims.add(lp);
            }
        }

        for (var lp: victims) {
            if (keep.contains(lp.descriptor().id())) {
                throw new PluginException(id, "disabling it would also disable [%s]".formatted(lp.descriptor().id()));
            }
        }

        Collections.reverse(victims); // dependents first
        for (var lp: victims) {
            notifyDestructing(lp, lp.hooks());
            destructPlugin(lp);
            enabledPlugins.remove(lp);
        }

        LOG.info("Disabled {}", ids(victims));
        return mkDescriptorsFor(victims);
    }

    /// Enables a plugin, first enabling any of its dependencies that aren't, in
    /// load order. Dependents that were disabled along with it stay disabled.
    ///
    /// This is all-or-nothing: if any plugin fails to enable, the ones this call
    /// already enabled are disabled again, in reverse.
    ///
    /// Each plugin's existing instance and injector are reused, so
    /// [Plugin#onPluginEnable()] can be called again after
    /// [Plugin#onPluginDestruct()].
    ///
    /// @param id the plugin to enable
    /// @return the plugins that were enabled, in enable order
    /// @throws PluginException if no plugin has that id, it's already enabled, or it or one of its dependencies fails to enable
    public synchronized List<Descriptor> enable(String id) {
        requireState(State.Enabled, "enable");

        var target = find(id);
        if (enabledPlugins.contains(target)) {
            throw new PluginException(id, "plugin is already enabled");
        }

        var needed = new HashSet<Class<? extends Plugin>>();
        collectDisabled(target, needed);

        var started = new ArrayList<LoadedPlugin>();
        for (var lp : loadedPlugins.values()) { // load order = dependency order
            if (!needed.contains(lp.type())) {
                continue;
            }

            try {
                lp.instance().onPluginEnable();
            } catch (Exception ex) {
                rollback(started);
                throw new PluginException(lp.descriptor().id(), "failed to enable", ex);
            }

            // On failure, notifyEnabled has already undone its hooks and destructed lp.
            if (!notifyEnabled(lp)) {
                rollback(started);
                throw new PluginException(lp.descriptor().id(), "a dependency rejected it; see the log");
            }

            enabledPlugins.add(lp); // its dependents are all disabled, so appending keeps the order valid
            started.add(lp);
            LOG.info("Enabled {}", lp.descriptor().id());
        }

        for (var lp : started) {
            try {
                lp.instance().onPluginsReady();
            } catch (Exception ex) {
                LOG.error("{} failed in onPluginsReady", lp.descriptor().id(), ex);
            }
        }

        return mkDescriptorsFor(started);
    }

    private void collectDisabled(LoadedPlugin lp, Set<Class<? extends Plugin>> out) {
        if (enabledPlugins.contains(lp) || !out.add(lp.type())) {
            return;
        }

        for (var dep : lp.descriptor().dependsOn()) {
            collectDisabled(loadedPlugins.get(dep), out);
        }
    }

    private void rollback(List<LoadedPlugin> started) {
        for (int i = started.size() - 1; i >= 0; i--) {
            var lp = started.get(i);
            notifyDestructing(lp, lp.hooks());
            destructPlugin(lp);
            enabledPlugins.remove(lp);
        }
    }

    private LoadedPlugin find(String id) {
        return loadedPlugins.values().stream()
                .filter(lp -> lp.descriptor().id().equals(id))
                .findFirst()
                .orElseThrow(() -> new PluginException(id, "no plugin with this id is loaded"));
    }

    /// Returns the loaded instance of a plugin class.
    ///
    /// @param cls  the plugin class
    /// @param <P>  the plugin type
    /// @return the plugin instance, or empty if it is not loaded
    public synchronized <P extends Plugin> Optional<P> getPlugin(Class<P> cls) {
        return Optional.ofNullable(loadedPlugins.get(cls)).map(lp -> cls.cast(lp.instance()));
    }

    /// Returns the descriptors of every loaded plugin, in load order. This includes
    /// plugins that failed to enable.
    ///
    /// @return an unmodifiable list of descriptors
    public synchronized List<Descriptor> getLoadedPlugins() {
        return mkDescriptorsFor(loadedPlugins.values());
    }

    /// Returns the descriptors of every plugin that enabled successfully, in enable order.
    /// @return an unmodifiable list of descriptors
    public synchronized List<Descriptor> getEnabledPlugins() {
        return mkDescriptorsFor(enabledPlugins);
    }

    @Override
    public void close() {
        this.destroyAll();
    }

    /// Validates descriptors and sorts plugins so that every plugin comes after its
    /// dependencies. Ties are broken by id, so the order is deterministic.
    private static List<Class<? extends Plugin>> resolveOrder(List<Class<? extends Plugin>> types)
            throws PluginException {
        var byId = new HashMap<String, Class<? extends Plugin>>();
        for (var type : types) {
            var desc = type.getAnnotation(Descriptor.class);
            if (desc == null) {
                throw new PluginException(
                        null, String.format("%s is not annotated with `@Descriptor`", type.getName()));
            }

            if (desc.id().isBlank()) {
                throw new PluginException(null, String.format("%s has a blank plugin ID", type.getName()));
            }

            var previous = byId.putIfAbsent(desc.id(), type);
            if (previous != null) {
                throw new PluginException(
                        desc.id(), String.format("id is used by both %s and %s", previous.getName(), type.getName()));
            }
        }

        var present = new HashSet<>(types);
        var dependents = new HashMap<Class<? extends Plugin>, Set<Class<? extends Plugin>>>();
        var inDegree = new HashMap<Class<? extends Plugin>, Integer>();

        for (var type : types) {
            inDegree.put(type, 0);

            var desc = type.getAnnotation(Descriptor.class);
            Class<? extends Plugin>[] dependencies;
            try {
                dependencies = desc.dependsOn();
            } catch (TypeNotPresentException ex) {
                throw new PluginException(
                        desc.id(),
                        String.format(
                                "plugin depends on [%s]; which is not available on the classpath", ex.typeName()));
            }

            for (var dep : new LinkedHashSet<>(Arrays.asList(dependencies))) {
                if (dep == type) {
                    throw new PluginException(desc.id(), "plugin depends on itself");
                }

                if (!present.contains(dep)) {
                    throw new PluginException(
                            desc.id(),
                            String.format("plugin depends on [%s]; which is not a loaded plugin", dep.getName()));
                }

                dependents.computeIfAbsent(dep, k -> new HashSet<>()).add(type);
                inDegree.merge(type, 1, Integer::sum);
            }
        }

        var ready = new PriorityQueue<Class<? extends Plugin>>(Comparator.comparing(PluginManager::idOf));
        inDegree.forEach((type, deg) -> {
            if (deg == 0) {
                ready.add(type);
            }
        });

        var order = new ArrayList<Class<? extends Plugin>>(types.size());
        while (!ready.isEmpty()) {
            var next = ready.poll();
            order.add(next);

            for (var dependent : dependents.getOrDefault(next, Set.of())) {
                if (inDegree.merge(dependent, -1, Integer::sum) == 0) {
                    ready.add(dependent);
                }
            }
        }

        if (order.size() != types.size()) {
            var cycle = inDegree.entrySet().stream()
                    .filter(e -> e.getValue() > 0)
                    .map(e -> idOf(e.getKey()))
                    .sorted()
                    .toList();

            throw new PluginException(null, "dependency cycle detected: " + cycle);
        }

        return order;
    }

    /// Builds a plugin's `pre` and `final` injectors. All of its
    /// dependencies must already be in [#loadedPlugins].
    private LoadedPlugin load(Class<? extends Plugin> type) {
        var desc = type.getAnnotation(Descriptor.class);
        var deps = Arrays.stream(desc.dependsOn())
                .distinct()
                .map(loadedPlugins::get)
                .toList();

        try {
            // This is Phase #1: what the plugin's constructor and @Inject members can see.
            var pre = coreInjector.createChildInjector(new DependencyBridgeModule(desc.id(), deps), binder -> {
                binder.requireExplicitBindings();
                binder.bind(Descriptor.class).toInstance(desc);
                bindSingleton(binder, type);
            });

            Plugin plugin = pre.getInstance(type);

            // Phase #2: plugin's own bindings
            var injector = pre.createChildInjector(plugin, Binder::requireExplicitBindings);
            plugin.attach(injector);

            var exports = new LinkedHashSet<Key<?>>(plugin.exports());
            for (var key : exports) {
                if (injector.getExistingBinding(key) == null) {
                    throw new PluginException(desc.id(), String.format("plugin exports [%s] but never binds it", key));
                }
            }

            exports.add(Key.get(type)); // dependents can inject the plugin itself

            var hooks = deps.stream()
                    .filter(dep -> dep.instance() instanceof PluginLifecycleHook)
                    .map(dep -> (PluginLifecycleHook) dep.instance())
                    .toList();

            return new LoadedPlugin(type, plugin, desc, injector, Set.copyOf(exports), hooks);
        } catch (PluginException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new PluginException(desc.id(), "failed to load plugin", ex);
        }
    }

    private static <T> void bindSingleton(Binder binder, Class<T> type) {
        binder.bind(type).in(Singleton.class);
    }

    private boolean notifyEnabled(LoadedPlugin plugin) {
        var notified = new ArrayList<PluginLifecycleHook>();
        for (var hook : plugin.hooks()) {
            try {
                hook.onDependentEnabled(plugin.context());
                notified.add(hook);
            } catch (Exception ex) {
                LOG.error(
                        "hook [{}] rejected {}; rolling back",
                        hookIdOf(hook),
                        plugin.descriptor().id(),
                        ex);
                notifyDestructing(plugin, notified);
                destructPlugin(plugin);

                return false;
            }
        }

        return true;
    }

    private static void notifyDestructing(LoadedPlugin plugin, List<PluginLifecycleHook> hooks) {
        for (var i = hooks.size() - 1; i >= 0; i--) {
            var hook = hooks.get(i);
            try {
                hook.onDependentDestructing(plugin.context());
            } catch (Exception ex) {
                LOG.error(
                        "hook [{}] failed while plugin [{}] was destructing",
                        hookIdOf(hook),
                        plugin.descriptor().id(),
                        ex);
            }
        }
    }

    private static void destructPlugin(LoadedPlugin plugin) {
        var id = plugin.descriptor().id();
        try {
            plugin.instance().onPluginDestruct();
            LOG.info("plugin [{}] has been destructed", id);
        } catch (Exception ex) {
            LOG.error("failed to destruct plugin [%s]:", ex);
        }
    }

    private void requireState(State expected, String operation) throws IllegalStateException {
        if (this.state != expected) {
            throw new IllegalStateException(String.format(
                    "operation `%s()` requires state [%s] but we are in state [%s]", operation, expected, this.state));
        }
    }

    private static String idOf(Class<? extends Plugin> type) {
        var desc = type.getAnnotation(Descriptor.class);
        return desc.id() != null ? desc.id() : type.getName();
    }

    private static String hookIdOf(PluginLifecycleHook hook) {
        return hook instanceof Plugin plugin
                ? idOf(plugin.getClass())
                : hook.getClass().getName();
    }

    private static List<String> ids(Iterable<LoadedPlugin> plugins) {
        var out = new ArrayList<String>();
        plugins.forEach(plugin -> out.add(plugin.descriptor().id()));

        return Collections.unmodifiableList(out);
    }

    private static List<Descriptor> mkDescriptorsFor(Iterable<LoadedPlugin> plugins) {
        var out = new ArrayList<Descriptor>();
        plugins.forEach(plugin -> out.add(plugin.descriptor()));

        return Collections.unmodifiableList(out);
    }

    /// Makes the exports of a plugin's direct dependencies injectable in its
    /// `pre` injector (and therefore its `final` injector).
    ///
    /// Each key is bound to a provider from the injector that owns it, so scoping
    /// is preserved: a singleton in a dependency is the same instance everywhere.
    /// Two dependencies exporting the same key is an error.
    private static final class DependencyBridgeModule extends AbstractModule {
        private final String pluginId;
        private final List<LoadedPlugin> dependencies;

        DependencyBridgeModule(String id, List<LoadedPlugin> dependencies) {
            this.pluginId = id;
            this.dependencies = dependencies;
        }

        @Override
        protected void configure() {
            var origins = new HashMap<Key<?>, LoadedPlugin>();
            for (var dep : this.dependencies) {
                for (var key : dep.exports()) {
                    var previous = origins.putIfAbsent(key, dep);
                    if (previous != null) {
                        throw new PluginException(
                                this.pluginId,
                                String.format(
                                        "cannot load plugin: %s is exported by both %s and %s",
                                        key,
                                        previous.descriptor().id(),
                                        dep.descriptor().id()));
                    }

                    bridge(key, dep.injector());
                }
            }
        }

        private <T> void bridge(Key<T> key, Injector from) {
            bind(key).toProvider(from.getProvider(key));
        }
    }
}
