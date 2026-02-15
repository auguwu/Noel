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

import com.google.inject.Inject;
import dev.floofy.noel.http.Server;
import dev.floofy.noel.http.server.HttpMethod;
import dev.floofy.noel.http.server.Router;
import dev.floofy.noel.modules.settings.Setting;
import dev.floofy.noel.modules.settings.Settings;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class NettyServerImpl implements Server {
    private static final Setting<Integer> HTTP_PORT = Setting.of("http.server.port", (value) -> {
        if (value == null) {
            throw new RuntimeException();
        }

        if (value instanceof Integer intValue) {
            return intValue;
        }

        try {
            return Integer.parseUnsignedInt((String)value);
        } catch (NumberFormatException e) {
            throw new RuntimeException("failed to parse integral value", e);
        }
    });

    private static final Setting<String> HTTP_ADDRESS = Setting.string("http.server.address", true);

    private final AtomicInteger inflightRequests = new AtomicInteger(0);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(NettyServerImpl.class);
    private final RouterImpl router = new RouterImpl();
    private final InetSocketAddress socketAddress;

    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1, new ThreadFactory() {
        private final AtomicInteger count = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            final Thread thread = new Thread(r, String.format("Noel-NettyBoss[%d]", count.addAndGet(1)));
            thread.setDaemon(true);

            return thread;
        }
    });

    private final EventLoopGroup workerGroup = new NioEventLoopGroup(0, new ThreadFactory() {
        private final AtomicInteger count = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            final Thread thread = new Thread(r, String.format("Noel-NettyWorker[%d]", count.addAndGet(1)));
            thread.setDaemon(true);

            return thread;
        }
    });

    @Inject
    NettyServerImpl(@NotNull Settings settings) {
        final var address = settings.getOrDefault(HTTP_ADDRESS, "0.0.0.0");
        final var port = settings.getOrDefault(HTTP_PORT, 3621);

        socketAddress = new InetSocketAddress(address, port);
        router.get("/", (req, res) -> {
            res.sendText("Hello, world!");
        });
    }

    @Override
    public int getInflightRequests() {
        return inflightRequests.get();
    }

    @Override
    public @NotNull Router getRouter() {
        return router;
    }

    public void incrementInflightRequests() {
        inflightRequests.incrementAndGet();
    }

    public void decrementInflightRequests() {
        inflightRequests.decrementAndGet();
    }

    @Override
    public void start() throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception {
                        channel.pipeline()
                                .addLast(new HttpServerCodec())
                                .addLast(new HttpObjectAggregator(65535))
                                .addLast(new Handler(NettyServerImpl.this));
                    }
                });

        log.info("Binding HTTP server to {}", socketAddress);

        ChannelFuture fut = bootstrap.bind(socketAddress).sync();
        fut.channel().closeFuture().sync();
    }

    @Override
    public void shutdown() throws Exception {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    static class Handler extends SimpleChannelInboundHandler<FullHttpRequest> {
        private final Logger log = LoggerFactory.getLogger(getClass());
        private final NettyServerImpl server;

        Handler(@NotNull NettyServerImpl server) {
            this.server = server;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws Exception {
            server.incrementInflightRequests();

            final SocketAddress remoteAddress = ctx.channel().remoteAddress();
            log.trace("Received incoming request from {}", remoteAddress);

            final var method = HttpMethod.fromNetty(httpRequest.method());
            final ResponseImpl response = new ResponseImpl(ctx, server.objectMapper);
            if (method == null) {
                final HashMap<String, Object> map = new HashMap<>();
                map.put("success", false);
                map.put("message", String.format("unknown HTTP method `%s'", httpRequest.method()));

                response.setStatus(HttpResponseStatus.METHOD_NOT_ALLOWED).sendJSON(map);
                server.decrementInflightRequests();

                return;
            }

            log.trace("START :: {} {}", method, httpRequest.uri());

            final boolean found = server.router.executeRequest(new RequestImpl(httpRequest, method, remoteAddress, server.objectMapper), response);
            if (!found && !response.isSent()) {
                final HashMap<String, Object> map = new HashMap<>();
                map.put("success", false);
                map.put("message", String.format("unknown route: %s %s", httpRequest.method(), httpRequest.uri()));

                response.setStatus(HttpResponseStatus.NOT_FOUND).sendJSON(map);
            }

            server.decrementInflightRequests();
        }
    }
}
