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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Guice;
import dev.floofy.noel.plugins.testing.RecordingPlugin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PluginManagerTest {
    public static final class Events {
        public final List<String> logs = Collections.synchronizedList(new ArrayList<>());

        public void add(String event) {
            logs.add(event);
        }
    }

    private PluginManager manager;
    private Events events;

    @BeforeEach
    void setUp() {
        this.events = new Events();
        var core = Guice.createInjector(binder -> {
            binder.bind(Events.class).toInstance(this.events);
        });

        this.manager = new PluginManager(core);
    }

    @SafeVarargs
    private static List<Class<? extends Plugin>> plugins(Class<? extends Plugin>... cls) {
        return List.of(cls);
    }

    private static List<String> ids(List<Descriptor> descriptors) {
        return descriptors.stream().map(Descriptor::id).toList();
    }

    @Test
    void loadsDependenciesBeforeDependents() {
        var order = manager.loadAll(plugins(RecordingPlugin.B.class, RecordingPlugin.A.class));
        assertThat(ids(order)).containsExactly("a", "b");
    }

    @Test
    void breaksTiesById() {
        var order = manager.loadAll(plugins(RecordingPlugin.Independent.class, RecordingPlugin.A.class));
        assertThat(ids(order)).containsExactly("a", "independent");
    }

    @Test
    void bridgesExportsAsTheSameSingleton() {
        manager.loadAll(plugins(RecordingPlugin.A.class, RecordingPlugin.B.class));
        var a = manager.getPlugin(RecordingPlugin.A.class).orElseThrow();
        var b = manager.getPlugin(RecordingPlugin.B.class).orElseThrow();

        assertThat(b.greeter).isSameAs(a.greeter());
    }

    @Test
    void dependentsCanInjectTheDependencyPlugin() {
        manager.loadAll(plugins(RecordingPlugin.A.class, RecordingPlugin.B.class));
        var a = manager.getPlugin(RecordingPlugin.A.class).orElseThrow();
        var b = manager.getPlugin(RecordingPlugin.B.class).orElseThrow();

        assertThat(b.a).isSameAs(a);
    }

    @Test
    void unexportedBindingsArePrivate() {
        // Guice can't satisfy SecretPeeker's constructor, so load() wraps the CreationException.
        assertThatThrownBy(() -> manager.loadAll(plugins(RecordingPlugin.A.class, RecordingPlugin.SecretPeeker.class)))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("secret-peeker")
                .hasMessageContaining("failed to load plugin");

        assertThat(manager.getLoadedPlugins()).isEmpty();
    }

    @Test
    void exportsAreNotTransitive() {
        assertThatThrownBy(() -> manager.loadAll(
                        plugins(RecordingPlugin.A.class, RecordingPlugin.B.class, RecordingPlugin.TransitiveC.class)))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("transitive-c")
                .hasMessageContaining("failed to load plugin");
    }

    @Test
    void rejectsExportsThatAreNeverBound() {
        assertThatThrownBy(() -> manager.loadAll(plugins(RecordingPlugin.UnboundExport.class)))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("unbound-export")
                .hasMessageContaining("plugin exports [")
                .hasMessageContaining("] but never binds it");
    }

    @Test
    void rejectsTwoDependenciesExportingTheSameKey() {
        // Thrown inside the bridge module's configure(), so it reaches us wrapped by Guice.
        assertThatThrownBy(() -> manager.loadAll(plugins(
                        RecordingPlugin.A.class,
                        RecordingPlugin.ConflictingExporter.class,
                        RecordingPlugin.ConflictDependent.class)))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("conflict-dependent")
                .hasMessageContaining("failed to load plugin")
                .hasStackTraceContaining("cannot load plugin: ")
                .hasStackTraceContaining("is exported by both a and conflicting-exporter");
    }

    @Test
    void rejectsCycles() {
        assertThatThrownBy(() -> manager.loadAll(plugins(RecordingPlugin.CycleX.class, RecordingPlugin.CycleY.class)))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("dependency cycle detected: [cycle-x, cycle-y]");
    }

    @Test
    void rejectsSelfDependency() {
        assertThatThrownBy(() -> manager.loadAll(plugins(RecordingPlugin.SelfDependent.class)))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("self")
                .hasMessageContaining("plugin depends on itself");
    }

    @Test
    void rejectsMissingDependency() {
        assertThatThrownBy(() -> manager.loadAll(plugins(RecordingPlugin.B.class)))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("b")
                .hasMessageContaining("plugin depends on [%s]; which is not a loaded plugin"
                        .formatted(RecordingPlugin.A.class.getName()));
    }

    @Test
    void rejectsDuplicateIds() {
        assertThatThrownBy(() -> manager.loadAll(plugins(RecordingPlugin.A.class, RecordingPlugin.DuplicateOfA.class)))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("id is used by both %s and %s"
                        .formatted(RecordingPlugin.A.class.getName(), RecordingPlugin.DuplicateOfA.class.getName()));
    }

    @Test
    void rejectsMissingDescriptor() {
        assertThatThrownBy(() -> manager.loadAll(plugins(RecordingPlugin.NoDescriptor.class)))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("%s is not annotated with `@Descriptor`"
                        .formatted(RecordingPlugin.NoDescriptor.class.getName()));
    }

    @Test
    void failedLoadLeavesNothingLoadedAndCanBeRetried() {
        assertThatThrownBy(() -> manager.loadAll(plugins(RecordingPlugin.CycleX.class, RecordingPlugin.CycleY.class)))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("dependency cycle detected");

        assertThat(manager.getLoadedPlugins()).isEmpty();
        assertThat(ids(manager.loadAll(plugins(RecordingPlugin.A.class)))).containsExactly("a");
    }

    @Test
    void enablesInOrderThenReadyThenDestructsInReverse() {
        manager.loadAll(plugins(RecordingPlugin.B.class, RecordingPlugin.A.class));
        manager.enableAll();
        manager.destroyAll();

        assertThat(events.logs)
                .containsExactly(
                        "enable:a", "enable:b",
                        "ready:a", "ready:b",
                        "destruct:b", "destruct:a");
    }

    @Test
    void nothingIsEnabledDuringLoad() {
        manager.loadAll(plugins(RecordingPlugin.A.class, RecordingPlugin.B.class));
        assertThat(events.logs).isEmpty();
    }

    @Test
    void failureSkipsDependentsButNotSiblings() {
        manager.loadAll(plugins(
                RecordingPlugin.Failing.class,
                RecordingPlugin.FailingDependent.class,
                RecordingPlugin.Independent.class));
        var enabled = manager.enableAll();
        manager.destroyAll();

        assertThat(ids(enabled)).containsExactly("independent");
        assertThat(events.logs)
                .containsExactly(
                        "enable:failing", // threw
                        "enable:independent", // failing-dependent was skipped
                        "ready:independent",
                        "destruct:independent"); // failing is never destructed
    }

    @Test
    void hooksSeeDependentsAfterEnableAndBeforeDestruct() {
        manager.loadAll(plugins(RecordingPlugin.HookHost.class, RecordingPlugin.HookClient.class));
        manager.enableAll();
        manager.destroyAll();

        assertThat(events.logs)
                .containsExactly(
                        "enable:hook-host",
                        "enable:hook-client",
                        "hook-enabled:hook-client",
                        "ready:hook-host",
                        "ready:hook-client",
                        "hook-destructing:hook-client",
                        "destruct:hook-client",
                        "destruct:hook-host");
    }

    @Test
    void rejectedDependentIsRolledBack() {
        manager.loadAll(plugins(
                RecordingPlugin.HookHost.class,
                RecordingPlugin.RejectingHost.class,
                RecordingPlugin.RejectedClient.class));
        var enabled = manager.enableAll();

        assertThat(ids(enabled)).containsExactly("hook-host", "rejecting-host");
        assertThat(events.logs)
                .containsExactly(
                        "enable:hook-host",
                        "enable:rejecting-host",
                        "enable:rejected-client",
                        "hook-enabled:rejected-client", // HookHost accepted it first...
                        "rejecting:rejected-client", // ...then RejectingHost threw
                        "hook-destructing:rejected-client", // HookHost is told to undo
                        "destruct:rejected-client",
                        "ready:hook-host",
                        "ready:rejecting-host");
    }

    @Test
    void enableBeforeLoadFails() {
        assertThatThrownBy(manager::enableAll)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("operation `enableAll()` requires state [Loaded] but we are in state [Uninit]");
    }

    @Test
    void loadTwiceFails() {
        manager.loadAll(plugins(RecordingPlugin.A.class));
        assertThatThrownBy(() -> manager.loadAll(plugins(RecordingPlugin.A.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("operation `loadAll()` requires state [Uninit] but we are in state [Loaded]");
    }

    @Test
    void destroyAllIsIdempotent() {
        manager.loadAll(plugins(RecordingPlugin.A.class));
        manager.enableAll();
        manager.destroyAll();
        manager.destroyAll();

        assertThat(events.logs).containsOnlyOnce("destruct:a");
        assertThat(manager.getLoadedPlugins()).isEmpty();
    }

    @Test
    void discoversAnnotatedPluginsIncludingInjectConstructors() {
        var found = PluginManager.discover(
                PluginManagerTest.class.getClassLoader(), PluginManagerTest.class.getPackageName());

        assertThat(found)
                .contains(RecordingPlugin.A.class, RecordingPlugin.B.class) // B only has an @Inject constructor
                .doesNotContain(RecordingPlugin.NoDescriptor.class, RecordingPlugin.class);
    }
}
