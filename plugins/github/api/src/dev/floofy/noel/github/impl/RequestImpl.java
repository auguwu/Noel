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

package dev.floofy.noel.github.impl;

import dev.floofy.noel.github.api.Method;
import dev.floofy.noel.github.api.Request;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import tools.jackson.core.type.TypeReference;

public final class RequestImpl<T> implements Request<T> {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final byte[] EMPTY = new byte[0];

    private final Function<byte[], T> decoder;
    private final Headers.Builder headers;
    private final HttpUrl.Builder url;
    private final ClientImpl client;
    private final Method method;
    private Object body;

    RequestImpl(ClientImpl client, Method method, HttpUrl.Builder url, Function<byte[], T> decoder) {
        this(client, method, url, decoder, new Headers.Builder(), null);
    }

    RequestImpl(
            ClientImpl client,
            Method method,
            HttpUrl.Builder url,
            Function<byte[], T> decoder,
            Headers.Builder headers,
            Object body) {
        this.headers = Objects.requireNonNull(headers, "headers");
        this.decoder = Objects.requireNonNull(decoder, "decoder");
        this.client = Objects.requireNonNull(client, "client");
        this.method = Objects.requireNonNull(method, "method");
        this.body = body;
        this.url = Objects.requireNonNull(url);
    }

    @Override
    public Request<T> addQueryParameter(String name, Object value, boolean encode) {
        String encodedValue = String.valueOf(Objects.requireNonNull(value, name));
        if (encode) {
            this.url.addEncodedQueryParameter(name, encodedValue);
        } else {
            this.url.addQueryParameter(name, encodedValue);
        }

        return this;
    }

    @Override
    public Request<T> header(String name, Object value) {
        this.headers.add(name, String.valueOf(Objects.requireNonNull(value, name)));
        return this;
    }

    @Override
    public Request<T> body(Object body) {
        if (!this.method.allowsBody()) {
            throw new IllegalStateException("%s requests can't have a body".formatted(method));
        }

        this.body = Objects.requireNonNull(body, "body");
        return this;
    }

    @Override
    public <R> Request<R> as(Class<R> type) {
        final var reader = this.client.getMapper().readerFor(type);
        return copyWith(reader::readValue);
    }

    @Override
    public <R> Request<R> as(TypeReference<R> type) {
        final var reader = this.client.getMapper().readerFor(type);
        return copyWith(reader::readValue);
    }

    private <R> RequestImpl<R> copyWith(Function<byte[], R> decoder) {
        return new RequestImpl<>(
                this.client,
                this.method,
                this.url.build().newBuilder(),
                decoder,
                new Headers.Builder().addAll(headers.build()),
                this.body);
    }

    @Override
    public CompletableFuture<T> submit() {
        final var request = new okhttp3.Request.Builder()
                .method(this.method.name(), requestBody())
                .url(this.url.build())
                .headers(this.headers.build())
                .build();

        return this.client.execute(request, this.decoder);
    }

    RequestBody requestBody() {
        if (this.body != null) {
            return RequestBody.create(this.client.getMapper().writeValueAsBytes(this.body), JSON);
        }

        // OkHttp throws for a POST, PUT or PATCH without a body, and some endpoints
        // (like starring a repository) are a bodiless PUT. GET must have none.
        return this.method.allowsBody() ? RequestBody.create(EMPTY, null) : null;
    }
}
