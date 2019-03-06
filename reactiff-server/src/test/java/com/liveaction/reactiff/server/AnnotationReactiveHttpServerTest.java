package com.liveaction.reactiff.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.Body;
import com.liveaction.reactiff.api.codec.CodecManager;
import com.liveaction.reactiff.api.server.ReactiveFilter;
import com.liveaction.reactiff.codec.CodecManagerImpl;
import com.liveaction.reactiff.codec.RawBinaryCodec;
import com.liveaction.reactiff.codec.RawFileCodec;
import com.liveaction.reactiff.codec.TextPlainCodec;
import com.liveaction.reactiff.codec.jackson.JsonCodec;
import com.liveaction.reactiff.codec.jackson.SmileBinaryCodec;
import com.liveaction.reactiff.server.example.AnnotationTestController;
import com.liveaction.reactiff.server.example.AuthFilter;
import com.liveaction.reactiff.server.example.TestController;
import com.liveaction.reactiff.server.example.api.Pojo;
import com.liveaction.reactiff.server.utils.ReactiveHttpServerTestUtils;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.test.StepVerifier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class AnnotationReactiveHttpServerTest {

    private static CodecManager codecManager;
    private static ReactiveHttpServer tested;

    @BeforeClass
    public static void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();

        codecManager = new CodecManagerImpl();
        codecManager.addCodec(new JsonCodec(objectMapper));
        codecManager.addCodec(new SmileBinaryCodec(objectMapper));
        codecManager.addCodec(new TextPlainCodec());
        codecManager.addCodec(new RawBinaryCodec());
        codecManager.addCodec(new RawFileCodec());

        ReactiveFilter corsFilter = DefaultFilters.cors(
                ImmutableSet.of("http://localhost"),
                ImmutableSet.of("X-UserToken"),
                ImmutableSet.of("GET", "POST", "PUT", "DELETE"),
                false,
                -1
        );

        ReactiveFilter exceptionMapping = DefaultFilters.exceptionMapping(throwable -> {
            if (throwable instanceof IllegalAccessException) {
                return 401;
            } else if (throwable instanceof NoSuchElementException) {
                return 404;
            } else {
                return null;
            }
        });
        tested = ReactiveHttpServer.create()
                .compress(true)
                .protocols(HttpProtocol.HTTP11)
                .codecManager(codecManager)
                .build();
        tested.addReactiveFilter(corsFilter);
        tested.addReactiveFilter(exceptionMapping);
        tested.addReactiveFilter(new AuthFilter());
        tested.addReactiveHandler(new AnnotationTestController());
        tested.start();
    }

    @AfterClass
    public static void after() {
        tested.close();
    }

    @Test
    public void shouldSendPathParameter() {
        StepVerifier.create(
                ReactiveHttpServerTestUtils.httpClient(tested)
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .get()
                        .uri("/annotated/params/true")
                        .response(ReactiveHttpServerTestUtils.checkErrorAndDecodeAsMono(Boolean.class)))
                .expectNext(true)
                .expectComplete()
                .verify();

        StepVerifier.create(
                ReactiveHttpServerTestUtils.httpClient(tested)
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .get()
                        .uri("/annotated/params/false")
                        .response(ReactiveHttpServerTestUtils.checkErrorAndDecodeAsMono(Boolean.class)))
                .expectNext(false)
                .expectComplete()
                .verify();
    }
}