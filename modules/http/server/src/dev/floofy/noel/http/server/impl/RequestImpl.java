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

import io.netty.handler.codec.http.FullHttpRequest;

import org.jetbrains.annotations.NotNull;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class RequestImpl implements Request {
    private final SocketAddress remoteAddress;
    private final ObjectMapper objectMapper;
    private final FullHttpRequest netty;
    private final HttpMethod method;

    protected RequestImpl(
            @NotNull FullHttpRequest request,
            @NotNull HttpMethod method,
            @NotNull SocketAddress remoteAddress,
            @NotNull ObjectMapper mapper) {
        this.remoteAddress = remoteAddress;
        objectMapper = mapper;
        this.method = method;
        this.netty = request;
    }

    @Override
    public @NotNull String getPath() {
        return netty.uri();
    }

    @Override
    public @NotNull HttpMethod getMethod() {
        return method;
    }

    @Override
    @NotNull
    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public @NotNull <T> T deserialize(Class<T> clazz) throws IOException {
        final String body = netty.content().toString(StandardCharsets.UTF_8);
        return objectMapper.readValue(body, clazz);
    }
}
