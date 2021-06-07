package com.liveaction.reactiff.server.performance;

import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.server.DefaultFilters;
import com.liveaction.reactiff.server.ReactiveHttpServer;
import com.liveaction.reactiff.server.performance.implementations.PerformanceController;
import com.liveaction.reactiff.server.rules.WithCodecManager;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

public final class PerformanceTest implements ReactiveHandler {
    static int i = 0;
    @ClassRule
    public static WithCodecManager withCodecManager = new WithCodecManager();

    public static ReactiveHttpServer server;
    public static HttpClient client;

    @BeforeClass
    public static void init() {
        server = ReactiveHttpServer.create()
                .compress(true)
                .displayRoutes(true)
                .protocols(HttpProtocol.HTTP11)
                .codecManager(withCodecManager.codecManager)
                .build();
        server.addReactiveFilter((DefaultFilters.exceptionMapping(throwable -> {
            if (throwable instanceof TimeoutException) return 404;
            if (throwable instanceof io.netty.handler.timeout.TimeoutException) return 401;
            return null;
        })));
        server.addReactiveHandler(new PerformanceController());
        server.start();

        ConnectionProvider connectionProvider = ConnectionProvider.builder("http")
                .maxConnections(500)
                .build();
        client = HttpClient.create(connectionProvider)
                .wiretap(true)
                .protocol(HttpProtocol.HTTP11)
                .baseUrl("http://localhost:" + server.port())
                .headers(httpHeaders -> httpHeaders.set(HttpHeaderNames.ACCEPT, "application/json"));
    }

    @AfterClass
    public static void destroy() {
        server.close();
    }

    @Test
    public void no_timeout() {
        Flux<String> data = Flux.just("str1", "str2").delayElements(Duration.ofMillis(20));
        client
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                .post()
                .uri("/post-timeout")
                .send(withCodecManager.codecManager.send("application/stream+json", data, String.class))
                .response(withCodecManager.codecManager.decodeAsFlux(Integer.class)).blockLast();
    }

    @Test
    public void timeout_delay_server() {
        Flux<String> data = Flux.just("str1", "str2");
        client
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                .post()
                .uri("/post-timeout-server")
                .send(withCodecManager.codecManager.send("application/stream+json", data, String.class))
                .response(withCodecManager.codecManager.decodeAsFlux(Integer.class)).blockLast();
    }

    @Test
    public void timeout_delay_entry() {
        Flux<String> data = Flux.just("str1", "str2").delayElements(Duration.ofMillis(200));
        client
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                .post()
                .uri("/post-timeout")
                .send(withCodecManager.codecManager.send("application/stream+json", data, String.class))
                .response(withCodecManager.codecManager.decodeAsFlux(Integer.class)).blockLast();
    }

    @Test
    public void cancel() {
        Flux<String> data = Flux.just("str1", "str2").delayElements(Duration.ofMillis(200));
        client
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                .post()
                .uri("/post-cancel")
                .send(withCodecManager.codecManager.send("application/stream+json", data, String.class))
                .response(withCodecManager.codecManager.decodeAsFlux(Integer.class)).blockLast();
    }
}