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

package dev.floofy.noel.birthdays;

import com.google.inject.Singleton;
import dev.floofy.noel.DiscordPlugin;
import dev.floofy.noel.MongoDBPlugin;
import dev.floofy.noel.PineconePlugin;
import dev.floofy.noel.birthdays.api.Scheduler;
import dev.floofy.noel.birthdays.api.Store;
import dev.floofy.noel.birthdays.impl.SchedulerImpl;
import dev.floofy.noel.birthdays.impl.StoreImpl;
import dev.floofy.noel.plugins.Descriptor;
import dev.floofy.noel.plugins.Plugin;

/// Birthday announcements, in each member's own timezone.
///
/// Members run `/birthday set` in a server. When their birthday starts in their
/// timezone (UTC unless they chose one), Noel announces it in that server's
/// configured channel:
///
/// ```yaml
/// birthdays:
///     "824066105102303232":
///         channel: "1001338219591585792"
///         role: "..."
/// ```
///
/// A server with no channel configured still stores birthdays, but announces nothing.
///
/// ## Lifecycle
/// - **Enable.** Makes sure the `nextAt` index exists, then starts the scheduler,
///   which checks for due birthdays right away and then once a minute.
/// - **Destruct.** Stops the scheduler and waits for a check that's already
///   running. Birthdays that come due while the plugin is disabled are announced
///   once it's enabled again, as long as the day hasn't ended in the member's
///   timezone.
///
/// Both can happen any number of times through `/plugins disable` and
/// `/plugins enable`, including when the MongoDB plugin is re-enabled underneath
/// this one: [Store] looks its collection up through the current client.
@Descriptor(
        id = "dev.floofy.noel.plugins.BirthdaysPlugin",
        description = "A fun and silly plugin to send happy birthday messages to users! <3",
        dependsOn = {DiscordPlugin.class, PineconePlugin.class, MongoDBPlugin.class})
public final class BirthdayPlugin extends Plugin {
    private Scheduler scheduler;

    @Override
    protected void configure() {
        bind(StoreImpl.class).in(Singleton.class);
        bind(SchedulerImpl.class).in(Singleton.class);

        bind(Scheduler.class).to(SchedulerImpl.class);
        bind(Store.class).to(StoreImpl.class);
    }

    @Override
    public void onPluginEnable() throws Exception {
        injector().getInstance(StoreImpl.class).createIndexes();

        this.scheduler = injector().getInstance(Scheduler.class);
        scheduler.start();
    }

    @Override
    public void onPluginDestruct() throws Exception {
        if (this.scheduler != null) {
            scheduler.close();
        }
    }
}
