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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Exact-match router, keyed by `METHOD:path`.
///
/// Concurrent, because plugins add and remove routes on the plugin manager's
/// thread while Netty's threads look them up.
public final class RouterImpl implements Router {
    private static final Logger LOG = LoggerFactory.getLogger(RouterImpl.class);
    private final ConcurrentMap<String, BiConsumer<Request, Response>> routes = new ConcurrentHashMap<>();

    /// Runs the handler for a request, if one is routed.
    /// @param request  the request
    /// @param response where the handler writes its reply
    /// @return `false` if no route matches
    public boolean executeRequest(Request request, Response response) {
        final var key = key(request.getMethod(), request.getPath());
        LOG.debug("executing route {} {} (key: [{}])", request.getMethod(), request.getPath(), key);

        final var handler = routes.get(key);
        if (handler == null) {
            LOG.warn(
                    "route [{} {}] was not found! did you register it in your plugin's `onPluginEnable'?",
                    request.getMethod(),
                    request.getPath());
            return false;
        }

        handler.accept(request, response);
        return true;
    }

    @Override
    public Registration route(HttpMethod method, String path, BiConsumer<Request, Response> handler) {
        final var key = key(method, path);
        if (routes.putIfAbsent(key, handler) != null) {
            throw new IllegalStateException("route %s %s is already registered".formatted(method, path));
        }

        return () -> routes.remove(key, handler);
    }

    private static String key(HttpMethod method, String path) {
        return method + ":" + path;
    }
}
