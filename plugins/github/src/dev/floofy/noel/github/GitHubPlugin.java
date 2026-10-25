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

package dev.floofy.noel.github;

import com.google.inject.Singleton;
import dev.floofy.noel.HttpClientPlugin;
import dev.floofy.noel.github.api.Client;
import dev.floofy.noel.github.impl.ClientImpl;
import dev.floofy.noel.plugins.Descriptor;
import dev.floofy.noel.plugins.Plugin;

@Descriptor(
        id = "dev.floofy.noel.plugins.GitHubPlugin",
        description = "Plugin that provides access to the GitHub API",
        dependsOn = {HttpClientPlugin.class})
public final class GitHubPlugin extends Plugin {
    @Override
    protected void configure() {
        bind(ClientImpl.class).in(Singleton.class);
        bind(Client.class).to(ClientImpl.class);

        export(Client.class);
    }
}
