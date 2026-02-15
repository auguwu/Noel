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

package dev.floofy.noel.modules;

import dev.floofy.noel.http.Server;
import dev.floofy.noel.http.server.impl.NettyServerImpl;
import dev.floofy.noel.modules.annotations.Initializer;
import dev.floofy.noel.modules.annotations.Module;
import dev.floofy.noel.modules.annotations.Teardown;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

@Module(name = "http:server", description = "A HTTP server that'll react on a separate thread", priority = 900)
public final class HttpServerModule extends AbstractNoelModule {
    @Override
    protected void configure() {
        bind(Server.class).to(NettyServerImpl.class);
    }

    @Initializer
    public void init(@NotNull Server server, @NotNull ExecutorService executorService) {
        executorService.submit(() -> {
            try {
                server.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Teardown
    public void teardown(@NotNull Server server) throws Exception {
        server.shutdown();
    }
}
