package dev.floofy.noel.plugins.testing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.floofy.noel.plugins.*;

public class RecordingPlugin extends Plugin {
    @Inject
    PluginManagerTest.Events events;

    public String id() {
        return descriptor().id();
    }

    @Override
    public void onPluginEnable() throws Exception {
        this.events.add(String.format("enable:%s", this.id()));
    }

    @Override
    public void onPluginDestruct() {
        this.events.add(String.format("destruct:%s", this.id()));
    }

    @Override
    public void onPluginsReady() {
        this.events.add(String.format("ready:%s", this.id()));
    }

    @Descriptor(id = "a")
    public static final class A extends RecordingPlugin {
        @Override
        protected void configure() {
            bind(Greeter.class).to(HelloGreeter.class).in(Singleton.class);
            export(Greeter.class);

            bind(Secret.class).in(Singleton.class);
        }

        public Greeter greeter() {
            return this.injector().getInstance(Greeter.class);
        }
    }

    @Descriptor(id = "b", dependsOn = A.class)
    public static final class B extends RecordingPlugin {
        public final Greeter greeter;
        public final A a;

        @Inject
        B(Greeter greeter, A a) {
            this.greeter = greeter;
            this.a = a;
        }
    }

    @Descriptor(id = "independent")
    public static final class Independent extends RecordingPlugin {}

    @Descriptor(id = "secret-peeker", dependsOn = A.class)
    public static final class SecretPeeker extends RecordingPlugin {
        @Inject
        SecretPeeker(Secret secret) {}
    }

    @Descriptor(id = "transitive-c", dependsOn = B.class)
    public static final class TransitiveC extends RecordingPlugin {
        @Inject
        TransitiveC(Greeter greeter) {} // exported by A, which C does not depend on
    }

    @Descriptor(id = "unbound-export")
    public static final class UnboundExport extends RecordingPlugin {
        @Override
        protected void configure() {
            export(Greeter.class);
        }
    }

    @Descriptor(id = "conflicting-exporter")
    public static final class ConflictingExporter extends RecordingPlugin {
        @Override
        protected void configure() {
            bind(Greeter.class).to(HelloGreeter.class);
            export(Greeter.class);
        }
    }

    @Descriptor(
            id = "conflict-dependent",
            dependsOn = {A.class, ConflictingExporter.class})
    public static final class ConflictDependent extends RecordingPlugin {}

    @Descriptor(id = "cycle-x", dependsOn = CycleY.class)
    public static final class CycleX extends RecordingPlugin {}

    @Descriptor(id = "cycle-y", dependsOn = CycleX.class)
    public static final class CycleY extends RecordingPlugin {}

    @Descriptor(id = "self", dependsOn = SelfDependent.class)
    public static final class SelfDependent extends RecordingPlugin {}

    @Descriptor(id = "a")
    public static final class DuplicateOfA extends RecordingPlugin {}

    public static final class NoDescriptor extends Plugin {}

    @Descriptor(id = "failing")
    public static final class Failing extends RecordingPlugin {
        @Override
        public void onPluginEnable() throws Exception {
            super.onPluginEnable();
            throw new IllegalStateException("boom");
        }
    }

    @Descriptor(id = "failing-dependent", dependsOn = Failing.class)
    public static final class FailingDependent extends RecordingPlugin {}

    @Descriptor(id = "hook-host")
    public static final class HookHost extends RecordingPlugin implements PluginLifecycleHook {
        @Override
        public void onDependentEnabled(PluginContext dependent) {
            events.add("hook-enabled:" + dependent.descriptor().id());
        }

        @Override
        public void onDependentDestructing(PluginContext dependent) {
            events.add("hook-destructing:" + dependent.descriptor().id());
        }
    }

    @Descriptor(id = "hook-client", dependsOn = HookHost.class)
    public static final class HookClient extends RecordingPlugin {}

    @Descriptor(id = "rejecting-host")
    public static final class RejectingHost extends RecordingPlugin implements PluginLifecycleHook {
        @Override
        public void onDependentEnabled(PluginContext dependent) {
            events.add("rejecting:" + dependent.descriptor().id());
            throw new IllegalStateException("rejected");
        }
    }

    @Descriptor(
            id = "rejected-client",
            dependsOn = {HookHost.class, RejectingHost.class})
    public static final class RejectedClient extends RecordingPlugin {}
}
