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

package dev.floofy.noel;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import dev.floofy.noel.plugins.Descriptor;
import dev.floofy.noel.plugins.Plugin;
import okhttp3.OkHttpClient;

@Descriptor(
        id = "dev.floofy.noel.plugins.HttpClientPlugin",
        description = "Plugin that exports `okhttp3.OkHttpClient` to plugins")
public final class HttpClientPlugin extends Plugin {
    @Override
    protected void configure() {
        export(OkHttpClient.class);
    }

    @Provides
    @Singleton
    OkHttpClient client() {
        return new OkHttpClient.Builder().followRedirects(true).build();
    }
}
