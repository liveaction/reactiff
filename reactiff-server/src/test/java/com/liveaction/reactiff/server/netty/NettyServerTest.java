package com.liveaction.reactiff.server.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.liveaction.reactiff.codec.CodecManager;
import com.liveaction.reactiff.codec.CodecManagerImpl;
import com.liveaction.reactiff.codec.TextPlainCodec;
import com.liveaction.reactiff.codec.json.JsonCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.test.StepVerifier;

import java.util.NoSuchElementException;
import java.util.function.BiFunction;

public class NettyServerTest {

    private static CodecManager codecManager;
    private static NettyServer tested;

    @BeforeClass
    public static void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();

        JsonCodec jsonCodec = new JsonCodec();
        jsonCodec.setObjectMapper(objectMapper);

        TextPlainCodec plainCodec = new TextPlainCodec();
        codecManager = new CodecManagerImpl();
        codecManager.addCodec(jsonCodec);
        codecManager.addCodec(plainCodec);

        TestController reactiveHandler = new TestController();
        reactiveHandler.setCodecManager(codecManager);

        TestController testController = new TestController();
        testController.setCodecManager(codecManager);

        ImmutableList<HttpProtocol> protocols = ImmutableList.of(HttpProtocol.HTTP11);
        ImmutableList<ReactiveHandler> handlers = ImmutableList.of(testController);
        ImmutableList<ReactiveFilter> reactiveFilters = ImmutableList.of(
                new ReactiveFilter() {
                    @Override
                    public Mono<Result<?>> filter(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse, FilterChain chain) {
                        return chain.chain(httpServerRequest, httpServerResponse)
                                .onErrorResume(throwable -> {
                                    boolean caught = false;
                                    int status = 500;
                                    if (throwable instanceof IllegalAccessException) {
                                        status = 401;
                                        caught = true;
                                    } else if (throwable instanceof NoSuchElementException) {
                                        status = 404;
                                        caught = true;
                                    }
                                    if (!caught) {
                                        LoggerFactory.getLogger(NettyServerTest.class).error("Unexpected error", throwable);
                                    }
                                    return Mono.just(Result.withCode(status, throwable.getMessage()));
                                });
                    }

                    @Override
                    public int rank() {
                        return -1;
                    }
                },
                new ReactiveFilter() {
                    @Override
                    public Mono<Result<?>> filter(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse, FilterChain chain) {
                        if (httpServerRequest.uri().startsWith("/yes")) {
                            return chain.chain(httpServerRequest, httpServerResponse);
                        } else {
                            return Mono.empty();
                        }
                    }

                    @Override
                    public int rank() {
                        return 0;
                    }
                }
        );
        tested = new NettyServer("0.0.0.0", -1, protocols, reactiveFilters, handlers, codecManager);
        tested.start();
    }

    @AfterClass
    public static void after() {
        tested.close();
    }

    @Test
    public void shouldReceiveStrings() {
        StepVerifier.create(httpClient()
                .headers(httpHeaders -> httpHeaders.set("Accept", "text/plain"))
                .get()
                .uri("/yes/Augustin")
                .responseContent()
                .asString())
                .expectNext("Hey Augustin")
                .expectNext("Hey baby !")
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldReceiveNoSuchElementException() {
        StepVerifier.create(httpClient()
                .get()
                .uri("/yes/nosuch")
                .response(decodeAs(String.class)))
                .expectErrorMessage("Not Found")
                .verify();
    }

    @Test
    public void shouldReceiveUnauthorized() {
        StepVerifier.create(httpClient()
                .get()
                .uri("/yes/unauthorized")
                .response(decodeAs(String.class)))
                .expectErrorMessage("Unauthorized")
                .verify();
    }

    private <T> BiFunction<HttpClientResponse, ByteBufFlux, Publisher<T>> decodeAs(Class<T> clazz) {
        return (response, flux) -> {
            HttpResponseStatus status = response.status();
            if (status.code() == 200) {
                return codecManager.decodeAs(clazz).apply(response, flux);
            } else {
                throw new RuntimeException(status.reasonPhrase());
            }
        };
    }

    private HttpClient httpClient() {
        return HttpClient.create()
                .protocol(HttpProtocol.HTTP11)
                .baseUrl("http://localhost:" + tested.port())
                .headers(httpHeaders -> httpHeaders.set(HttpHeaderNames.ACCEPT, "application/json"));
    }

}