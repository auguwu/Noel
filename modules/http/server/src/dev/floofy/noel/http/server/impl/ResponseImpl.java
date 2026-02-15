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

import dev.floofy.noel.http.server.Response;
import dev.floofy.noel.http.server.ResponseAlreadyOutgoingException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ResponseImpl implements Response {
    private final HttpHeaders headers = new DefaultHttpHeaders();
    private final ChannelHandlerContext context;
    private final ObjectMapper objectMapper;

    private HttpResponseStatus status = HttpResponseStatus.OK;
    private boolean sent = false;

    protected ResponseImpl(@NotNull ChannelHandlerContext context, @NotNull ObjectMapper mapper) {
        this.context = context;
        objectMapper = mapper;
    }

    void checkIfSent() {
        if (sent) {
            throw new ResponseAlreadyOutgoingException();
        }
    }

    public boolean isSent() {
        return sent;
    }

    @Override
    public Response setStatus(HttpResponseStatus status) {
        checkIfSent();

        this.status = status;
        return this;
    }

    @Override
    public Response setHeader(String name, Object value) throws ResponseAlreadyOutgoingException {
        checkIfSent();

        headers.set(name, value);
        return this;
    }

    @Override
    public void sendText(String text) throws ResponseAlreadyOutgoingException {
        checkIfSent();

        final FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer(text, StandardCharsets.UTF_8)
        );

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8").set(headers);
        context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

        sent = true;
    }

    @Override
    public void sendJSON(Object obj) throws IOException {
        checkIfSent();

        final String json = objectMapper.writeValueAsString(obj);
        final FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer(json, StandardCharsets.UTF_8)
        );

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8").set(headers);
        context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

        sent = true;
    }
}
