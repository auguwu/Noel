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
import dev.floofy.noel.Setting;
import dev.floofy.noel.Settings;
import dev.floofy.noel.http.Server;
import dev.floofy.noel.http.server.HttpMethod;
import dev.floofy.noel.http.server.Router;
import dev.floofy.noel.utils.Strings;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// A [Server] built on Netty.
///
/// The event loops are created by [#start()] and stopped by [#shutdown()], not
/// when this object is constructed, so the server can be started again after
/// its plugin is disabled and re-enabled.
public final class NettyServerImpl implements Server {
    private static final Logger LOG = LoggerFactory.getLogger(NettyServerImpl.class);
    private static final Setting<Integer> HTTP_PORT = Setting.integer("plugins.http.server.port");
    private static final Setting<String> HTTP_ADDRESS = Setting.string("plugins.http.server.address");

    /// How long [#shutdown()] waits for each event loop to stop.
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 5;

    private final AtomicInteger inflightRequests = new AtomicInteger(0);
    private final RouterImpl router = new RouterImpl();
    private final String address;
    private final int port;

    private EventLoopGroup workerGroup;
    private EventLoopGroup bossGroup;
    private Channel channel;

    @Inject
    NettyServerImpl(Settings settings) {
        this.address = settings.get(HTTP_ADDRESS).orElse("127.0.0.1");
        this.port = settings.get(HTTP_PORT).orElse(3621);

        router.get("/", (req, res) -> res.sendText("Hello, world!"));
    }

    @Override
    public int getInflightRequests() {
        return inflightRequests.get();
    }

    @Override
    public Router getRouter() {
        return router;
    }

    @Override
    public synchronized void start() throws Exception {
        if (this.channel != null) {
            throw new IllegalStateException(
                    String.format("HTTP server is already running on %s", channel.localAddress()));
        }

        final var addr = new InetSocketAddress(address, port);
        LOG.info("bootstrapping HTTP server with address {}", addr);

        this.bossGroup = new MultiThreadIoEventLoopGroup(
                1, new DefaultThreadFactory("Noel-NettyBoss", true), NioIoHandler.newFactory());

        this.workerGroup = new MultiThreadIoEventLoopGroup(
                0, new DefaultThreadFactory("Noel-NettyWorker", true), NioIoHandler.newFactory());

        try {
            this.channel = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline()
                                    .addLast(new HttpServerCodec())
                                    .addLast(new LogOutboundHandler())
                                    .addLast(new HttpObjectAggregator(65535))
                                    .addLast(new Handler(NettyServerImpl.this));
                        }
                    })
                    .bind(addr)
                    .sync()
                    .channel();
        } catch (Exception ex) {
            stopEventLoops();
            throw ex;
        }

        LOG.info("HTTP server is now listening at {}", channel.localAddress());
    }

    @Override
    public synchronized void shutdown() throws Exception {
        if (this.channel == null) {
            return;
        }

        try {
            this.channel.close().sync();
        } finally {
            this.channel = null;
            stopEventLoops();

            LOG.warn("HTTP server has stopped!");
        }
    }

    private void stopEventLoops() {
        final var boss = bossGroup.shutdownGracefully(0, SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        final var worker = workerGroup.shutdownGracefully(0, SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        boss.syncUninterruptibly();
        worker.syncUninterruptibly();

        this.bossGroup = null;
        this.workerGroup = null;
    }

    static final class LogOutboundHandler extends ChannelOutboundHandlerAdapter {
        private static final Logger LOG = LoggerFactory.getLogger(NettyServerImpl.class);

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof FullHttpResponse res && LOG.isTraceEnabled()) {
                LOG.trace("{} :: {}", res.status(), res.content().toString(StandardCharsets.UTF_8));
            }

            ctx.write(msg, promise);
        }
    }

    static final class Handler extends SimpleChannelInboundHandler<FullHttpRequest> {
        private static final Logger LOG = LoggerFactory.getLogger(Handler.class);
        private final NettyServerImpl server;

        Handler(NettyServerImpl server) {
            this.server = server;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext cx, FullHttpRequest httpRequest) throws Exception {
            server.inflightRequests.incrementAndGet();
            try {
                handle(cx, httpRequest);
            } finally {
                server.inflightRequests.decrementAndGet();
            }
        }

        void handle(ChannelHandlerContext cx, FullHttpRequest httpRequest) throws Exception {
            final var remoteAddress = cx.channel().remoteAddress();
            LOG.trace("received incoming request from {}", remoteAddress);

            final var method = HttpMethod.fromNetty(httpRequest.method());
            final var response = new ResponseImpl(cx);
            if (method == null) {
                response.setStatus(HttpResponseStatus.METHOD_NOT_ALLOWED)
                        .sendJSON(error("unknown HTTP method `%s'".formatted(httpRequest.method())));

                return;
            }

            final var now = System.nanoTime();
            LOG.trace("START :: {} {}", method, httpRequest.uri());
            final boolean found;
            try {
                found = server.router.executeRequest(new RequestImpl(httpRequest, method, remoteAddress), response);
            } catch (Exception ex) {
                // Without this, the client never got a reply and waited until it timed out.
                LOG.error("handler for {} {} failed:", method, httpRequest.uri(), ex);
                if (!response.isSent()) {
                    response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
                            .sendJSON(error("internal server error"));
                }

                return;
            }

            if (!found && !response.isSent()) {
                response.setStatus(HttpResponseStatus.NOT_FOUND)
                        .sendJSON(error("unknown route: %s %s".formatted(httpRequest.method(), httpRequest.uri())));
            }

            LOG.debug(
                    "END :: {} {} ~ took {}",
                    method,
                    httpRequest.uri(),
                    Strings.format(Duration.of(System.nanoTime() - now, ChronoUnit.NANOS)));
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            LOG.debug(
                    "closing connection from {} after an error:", ctx.channel().remoteAddress(), cause);

            ctx.close();
        }

        private static Map<String, Object> error(String message) {
            return Map.of("success", false, "error", message);
        }
    }
}
