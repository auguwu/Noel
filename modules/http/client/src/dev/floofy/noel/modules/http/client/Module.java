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

package dev.floofy.noel.modules.http.client;

import dev.floofy.noel.modules.AbstractNoelModule;

import okhttp3.OkHttpClient;

@dev.floofy.noel.modules.annotations.Module(
        name = "http:client",
        description = "HTTP client module that allows an accessible `OkHttpClient`",
        priority = 750)
public final class Module extends AbstractNoelModule {
    @Override
    protected void configure() {
        bind(OkHttpClient.class).toProvider(Provider.class);
    }

    static class Provider implements com.google.inject.Provider<OkHttpClient> {
        @Override
        public OkHttpClient get() {
            return new OkHttpClient.Builder().followRedirects(true).build();
        }
    }
}
