package com.liveaction.reactiff.server.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.liveaction.reactiff.codec.CodecManager;
import com.liveaction.reactiff.codec.CodecManagerImpl;
import com.liveaction.reactiff.codec.TextPlainCodec;
import com.liveaction.reactiff.codec.json.JsonCodec;
import com.liveaction.reactiff.server.netty.example.AuthFilter;
import com.liveaction.reactiff.server.netty.example.TestController;
import com.liveaction.reactiff.server.netty.example.api.Pojo;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;

public class ReactiveHttpServerTest {

    private static CodecManager codecManager;
    private static ReactiveHttpServer tested;

    @BeforeClass
    public static void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();

        JsonCodec jsonCodec = new JsonCodec();
        jsonCodec.setObjectMapper(objectMapper);

        TextPlainCodec plainCodec = new TextPlainCodec();
        codecManager = new CodecManagerImpl();
        codecManager.addCodec(jsonCodec);
        codecManager.addCodec(plainCodec);

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
                .protocols(HttpProtocol.HTTP11)
                .codecManager(codecManager)
                .build();
        tested.addReactiveFilter(corsFilter);
        tested.addReactiveFilter(exceptionMapping);
        tested.addReactiveFilter(new AuthFilter());
        tested.addReactiveHandler(new TestController());
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
                .expectErrorMessage("404 : Not Found")
                .verify();
    }

    @Test
    public void shouldReceiveUnauthorized() {
        StepVerifier.create(httpClient()
                .get()
                .uri("/yes/unauthorized")
                .response(decodeAs(String.class)))
                .expectErrorMessage("401 : Unauthorized")
                .verify();
    }

    @Test
    public void shouldPostAndReceivePojo() {
        StepVerifier.create(
                httpClient()
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .post()
                        .uri("/yes")
                        .send(codecManager.send("application/json", Mono.just(new Pojo("haroun", "tazieff"))))
                        .response(decodeAs(Pojo.class)))
                .expectNext(new Pojo("haroun", "tazieff from server"))
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldPostAndReceivePojo_flux() {
        StepVerifier.withVirtualTime(() ->
        {
            Flux<Pojo> just = Flux.just(new Pojo("haroun", "tazieff"),
                    new Pojo("haroun", "tazieff2"))
                    .delayElements(Duration.ofMillis(1000));
            return httpClient()
                    .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                    .post()
                    .uri("/yes")
                    .send(codecManager.send("application/json", just))
                    .response(decodeAs(Pojo.class));
        })
                .expectSubscription()
                .expectNoEvent(Duration.ofMillis(1000))
                .expectNext(new Pojo("haroun", "tazieff from server"))
                .expectNoEvent(Duration.ofMillis(1000))
                .expectNext(new Pojo("haroun", "tazieff2 from server"))
                .expectComplete()
                .verify(Duration.ofMillis(1000));
    }

    @Test
    public void shouldReceiveNotFoundWhenNoRouteMatch() {
        StepVerifier.create(httpClient()
                .get()
                .uri("/yes_not_exists")
                .response(decodeAs(String.class)))
                .expectErrorMessage("404 : Not Found")
                .verify();
    }

    @Test
    public void shouldHandlePreflightCORSRequest() {
        StepVerifier.create(httpClient()
                .headers(httpHeaders -> {
                    httpHeaders.set("Origin", "http://localhost");
                    httpHeaders.set("Access-Control-Request-Method", "GET");
                    httpHeaders.set("X-UserToken", "XXXXX");
                })
                .options()
                .uri("/yes")
                .response())
                .expectNextMatches(response -> {
                    String origin = response.responseHeaders().get("Access-Control-Allow-Origin");
                    String headers = response.responseHeaders().get("Access-Control-Allow-Headers");
                    String methods = response.responseHeaders().get("Access-Control-Allow-Methods");
                    return "http://localhost".equals(origin) &&
                            "Accept,Accept-Language,Content-Language,Content-Type,X-UserToken".equals(headers) &&
                            "DELETE,POST,GET,PUT".equals(methods);
                })
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldHandlerWebSocket() {
        Flux<WebSocketFrame> frames = httpClient()
                .baseUrl("ws://localhost:" + tested.port())
                .wiretap(true)
                .websocket()
                .uri("/websocket")
                .handle((websocketInbound, websocketOutbound) -> websocketInbound.receiveFrames());

        StepVerifier.create(frames)
                .expectNext(new TextWebSocketFrame("Salut !"))
                .expectNext(new TextWebSocketFrame("Je m'appelle"))
                .expectNext(new TextWebSocketFrame("Jean Baptiste Poquelin"))
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldNotFilterAuthorized() {
        StepVerifier.create(httpClient()
                .get()
                .uri("/oui")
                .response(decodeAs(String.class)))
                .expectNext("oui")
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldFilterUnauthorized() {
        StepVerifier.create(httpClient()
                .get()
                .uri("/non")
                .response(decodeAs(String.class)))
                .expectErrorMessage("401 : Unauthorized")
                .verify();
    }

    private <T> BiFunction<HttpClientResponse, ByteBufFlux, Publisher<T>> decodeAs(Class<T> clazz) {
        return (response, flux) -> {
            HttpResponseStatus status = response.status();
            if (status.code() == 200) {
                return codecManager.decodeAs(clazz).apply(response, flux);
            } else {
                return Mono.error(new HttpException(status.code(), status.code() + " : " + status.reasonPhrase()));
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