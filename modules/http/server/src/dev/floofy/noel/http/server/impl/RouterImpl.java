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

package dev.floofy.noel.http.server.impl;

import dev.floofy.noel.http.server.HttpMethod;
import dev.floofy.noel.http.server.Request;
import dev.floofy.noel.http.server.Response;
import dev.floofy.noel.http.server.Router;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.function.BiConsumer;

public final class RouterImpl implements Router {
    private final HashMap<String, BiConsumer<Request, Response>> routes = new HashMap<>();

    public boolean executeRequest(@NotNull Request request, @NotNull Response response) {
        final var handler =
                routes.get(String.format("%s:%s", request.getMethod(), request.getPath()));
        if (handler == null) {
            return false;
        }

        handler.accept(request, response);
        return true;
    }

    @Override
    public void route(HttpMethod method, String path, BiConsumer<Request, Response> consumer) {
        routes.putIfAbsent(String.format("%s:%s", method, path), consumer);
    }
}
