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
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import java.net.SocketAddress;
import java.util.Optional;

public class RequestImpl implements Request {
    private final SocketAddress remoteAddress;
    private final QueryStringDecoder uri;
    private final FullHttpRequest netty;
    private final HttpMethod method;

    protected RequestImpl(FullHttpRequest request, HttpMethod method, SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
        this.method = method;
        this.netty = request;
        this.uri = new QueryStringDecoder(request.uri());
    }

    @Override
    public String getPath() {
        return uri.path();
    }

    @Override
    public HttpMethod getMethod() {
        return method;
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public Optional<String> getQueryParameter(String name) {
        final var values = uri.parameters().get(name);
        return values == null || values.isEmpty() ? Optional.empty() : Optional.of(values.getFirst());
    }

    @Override
    public Optional<String> getHeader(String name) {
        return Optional.ofNullable(netty.headers().get(name));
    }

    @Override
    public byte[] getBody() {
        return ByteBufUtil.getBytes(netty.content());
    }
}
