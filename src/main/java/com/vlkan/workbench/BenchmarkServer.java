package com.vlkan.workbench;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.epoll.Epoll;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServer;

import java.time.Duration;
import java.util.concurrent.Callable;

public class BenchmarkServer implements Callable<Integer> {

    static { Epoll.ensureAvailability(); }

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkServer.class);

    private final String host;

    private final int port;

    private final int responsePayloadLength;

    public BenchmarkServer(String host, int port, int responsePayloadLength) {
        this.host = host;
        this.port = port;
        this.responsePayloadLength = responsePayloadLength;
        LOGGER.info("host = {}", host);
        LOGGER.info("port = {}", port);
        LOGGER.info("responsePayloadLength = {}", responsePayloadLength);
    }

    @Override
    public Integer call() {

        Epoll.ensureAvailability();

        LOGGER.info("building response payloads");
        byte[] responsePayload = new byte[responsePayloadLength];
        for (int i = 0; i < responsePayloadLength; i++) {
            responsePayload[i] = (byte) (i % Byte.MAX_VALUE);
        }

        LOGGER.info("starting server");
        HttpServer
                .create()
                .route(routes -> routes
                        .post("/", (request, response) -> request
                                .receive()
                                .then(Mono.defer(() -> {
                                    ByteBuf responsePayloadByteBuf = Unpooled.wrappedBuffer(responsePayload);
                                    Mono<ByteBuf> responsePayloadByteBufMono = Mono.just(responsePayloadByteBuf);
                                    return response
                                            .header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                                            .send(responsePayloadByteBufMono)
                                            .then();
                                }))))
                .host(host)
                .port(port)
                .bindUntilJavaShutdown(
                        Duration.ofHours(1),
                        ignored -> LOGGER.info("started server"));

        return 0;

    }

    public static void main(String[] args) {
        String host = BenchmarkHelpers.getStringProperty("benchmark.host", BenchmarkConstants.DEFAULT_SERVER_HOST);
        int port = BenchmarkHelpers.getIntProperty("benchmark.port", BenchmarkConstants.DEFAULT_SERVER_PORT);
        int responsePayloadLength = BenchmarkHelpers.getIntProperty("benchmark.responsePayloadLength", 1024);
        BenchmarkServer server = new BenchmarkServer(host, port, responsePayloadLength);
        int exitCode = server.call();
        System.exit(exitCode);
    }

}
