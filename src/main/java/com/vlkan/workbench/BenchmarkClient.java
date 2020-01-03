package com.vlkan.workbench;

import io.netty.channel.epoll.Epoll;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.Callable;

public class BenchmarkClient implements Callable<Integer> {

    static { Epoll.ensureAvailability(); }

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkClient.class);

    private static final String DEFAULT_BASE_URL =
            "http://" + BenchmarkConstants.DEFAULT_SERVER_HOST + ':' + BenchmarkConstants.DEFAULT_SERVER_PORT;

    private final String baseUrl;

    private final int concurrency;

    private final int periodSecs;

    public BenchmarkClient(String baseUrl, int concurrency, int periodSecs) {
        this.baseUrl = baseUrl;
        this.concurrency = concurrency;
        this.periodSecs = periodSecs;
        LOGGER.info("baseUrl = {}", baseUrl);
        LOGGER.info("concurrency = {}", concurrency);
        LOGGER.info("periodSecs = {}", periodSecs);
    }

    @Override
    public Integer call() {
        LOGGER.info("starting");
        HttpClient httpClient = HttpClient.create();
        Duration period = Duration.ofSeconds(periodSecs);
        Integer totalResponsePayloadLength = requestAll(httpClient)
                .doOnError(error -> LOGGER.error("request failure", error))
                .retry()
                .take(period)
                .reduce(0, Integer::sum)
                .block();
        assert totalResponsePayloadLength != null;
        LOGGER.info("completed (totalResponsePayloadLength={})", totalResponsePayloadLength);
        return 0;
    }

    private Flux<Integer> requestAll(HttpClient httpClient) {
        return Flux
                .range(0, Integer.MAX_VALUE)
                .flatMap(ignored -> requestOne(httpClient), concurrency)
                .checkpoint("requestAll");
    }

    private Mono<Integer> requestOne(HttpClient httpClient) {
        return httpClient
                .post()
                .uri(baseUrl)
                .responseSingle((response, responsePayloadByteBufMono) -> {

                    // Check the response status.
                    HttpResponseStatus responseStatus = response.status();
                    if (!is2xxSuccessful(responseStatus)) {
                        String message = String.format("unexpected response (responseStatus=%s)", responseStatus);
                        throw new RuntimeException(message);
                    }

                    // Read the response payload.
                    return responsePayloadByteBufMono
                            .asByteArray()
                            .map(responsePayloadBytes -> responsePayloadBytes.length)
                            .checkpoint("requestOne::read");

                })
                .checkpoint("requestOne");
    }

    private static boolean is2xxSuccessful(HttpResponseStatus status) {
        int statusCode = status.code();
        int statusCodeSeries = statusCode / 100;
        return statusCodeSeries == 2;
    }

    public static void main(String[] args) {
        String baseUrl = BenchmarkHelpers.getStringProperty("benchmark.baseUrl", DEFAULT_BASE_URL);
        int concurrency = BenchmarkHelpers.getIntProperty("benchmark.concurrency", 2);
        int periodSecs  = BenchmarkHelpers.getIntProperty("benchmark.periodSecs", 30);
        BenchmarkClient client = new BenchmarkClient(baseUrl, concurrency, periodSecs);
        int exitCode = client.call();
        System.exit(exitCode);
    }

}
