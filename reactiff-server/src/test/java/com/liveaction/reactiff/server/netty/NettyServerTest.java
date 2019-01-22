package com.liveaction.reactiff.server.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liveaction.reactiff.codec.CodecManager;
import com.liveaction.reactiff.codec.CodecManagerImpl;
import com.liveaction.reactiff.codec.TextPlainCodec;
import com.liveaction.reactiff.codec.json.JsonCodec;
import com.liveaction.reactiff.server.netty.example.AuthFilter;
import com.liveaction.reactiff.server.netty.example.ExceptionMappingFilter;
import com.liveaction.reactiff.server.netty.example.TestController;
import com.liveaction.reactiff.server.netty.example.api.Pojo;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.test.StepVerifier;

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

        tested = NettyServer.create()
                .protocols(HttpProtocol.HTTP11)
                .codecManager(codecManager)
                .filter(new ExceptionMappingFilter())
                .filter(new AuthFilter())
                .handler(new TestController())
                .build();
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

    @Test
    public void shouldPostAndReceivePojo() {
        StepVerifier.create(
                httpClient()
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .post()
                        .uri("/yes")
                        .send(codecManager.send(Mono.just(new Pojo("haroun", "tazieff"))))
                        .response(decodeAs(Pojo.class)))
                .expectNext(new Pojo("haroun", "tazieff from server"))
                .expectComplete()
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