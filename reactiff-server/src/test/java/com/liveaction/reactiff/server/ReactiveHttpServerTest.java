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
import com.liveaction.reactiff.codec.TextPlainCodec;
import com.liveaction.reactiff.codec.json.JsonCodec;
import com.liveaction.reactiff.server.example.AuthFilter;
import com.liveaction.reactiff.server.example.TestController;
import com.liveaction.reactiff.server.example.api.Pojo;
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

public class ReactiveHttpServerTest {

    private static CodecManager codecManager;
    private static ReactiveHttpServer tested;

    @BeforeClass
    public static void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();

        JsonCodec jsonCodec = new JsonCodec();
        jsonCodec.setObjectMapper(objectMapper);

        codecManager = new CodecManagerImpl();
        codecManager.addCodec(jsonCodec);
        codecManager.addCodec(new TextPlainCodec());
        codecManager.addCodec(new RawBinaryCodec());

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
                .wiretap(true)
                .compress(true)
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
                .response(decodeAsFlux(String.class)))
                .expectErrorMessage("404 : Not Found")
                .verify();
    }

    @Test
    public void shouldReceiveUnauthorized() {
        StepVerifier.create(httpClient()
                .get()
                .uri("/yes/unauthorized")
                .response(decodeAsFlux(String.class)))
                .expectErrorMessage("401 : Unauthorized")
                .verify();
    }

    @Test
    public void shouldPostAndReceivePojoUsingStreamJson() {
        StepVerifier.create(
                httpClient()
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/stream+json"))
                        .post()
                        .uri("/yes")
                        .send(codecManager.send("application/stream+json", Mono.just(new Pojo("haroun", "tazieff")), Pojo.class))
                        .response(decodeAsFlux(Pojo.class)))
                .expectNext(new Pojo("haroun", "tazieff from server"))
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldPostAndReceivePojoUsingStreamJson_flux() {
        StepVerifier.withVirtualTime(() ->
        {
            Flux<Pojo> just = Flux.just(new Pojo("haroun", "tazieff"),
                    new Pojo("haroun", "tazieff2"))
                    .delayElements(Duration.ofMillis(1000));
            return httpClient()
                    .headers(httpHeaders -> httpHeaders.set("Accept", "application/stream+json"))
                    .post()
                    .uri("/yes")
                    .send(codecManager.send("application/stream+json", just, Pojo.class))
                    .response(decodeAsFlux(Pojo.class));
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
    public void shouldPostAndReceivePojo() {
        StepVerifier.create(
                httpClient()
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .post()
                        .uri("/yes")
                        .send(codecManager.send("application/json", Flux.just(new Pojo("haroun", "tazieff")), Pojo.class))
                        .response(decodeAsFlux(Pojo.class)))
                .expectNext(new Pojo("haroun", "tazieff from server"))
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldPostAndReceivePojo_flux() {
        Flux<Pojo> just = Flux.just(new Pojo("haroun", "tazieff"),
                new Pojo("haroun", "tazieff2"));
        Flux<Pojo> actual = httpClient()
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                .post()
                .uri("/yes")
                .send(codecManager.send("application/json", just, Pojo.class))
                .response(decodeAsFlux(Pojo.class));

        StepVerifier.create(actual)
                .expectNext(new Pojo("haroun", "tazieff from server"))
                .expectNext(new Pojo("haroun", "tazieff2 from server"))
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldPostAndReceiveXMLFile() {
        Body<byte[]> body = readFileAsFlux("/test-xml-file.xml");
        Flux<byte[]> actual = httpClient()
                .wiretap(true)
                .post()
                .uri("/upload")
                .send(codecManager.send("text/xml", body))
                .response(decodeAsFlux(byte[].class));

        File expected = new File(getClass().getResource("/test-xml-file.xml").getFile());
        StepVerifier.create(asString(actual))
                .assertNext(content -> {
                    try {
                        assertThat(content).isEqualTo(Files.toString(expected, Charsets.UTF_8));
                    } catch (IOException e) {
                        fail("Unable to read expected file : " + e);
                    }
                })
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldPostAndReceiveBianryFile() {
        Body<byte[]> body = readFileAsFlux("/sample.pdf");
        Flux<byte[]> actual = httpClient()
                .wiretap(true)
                .post()
                .uri("/upload")
                .send(codecManager.send("application/pdf", body))
                .response(decodeAsFlux(byte[].class));

        File expected = new File(getClass().getResource("/sample.pdf").getFile());
        StepVerifier.create(asBinary(actual))
                .assertNext(content -> {
                    try {
                        assertThat(content).isEqualTo(Files.toByteArray(expected));
                    } catch (IOException e) {
                        fail("Unable to read expected file : " + e);
                    }
                })
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldPostAndReceivePojo_flux_withCompression() {
        Flux<Pojo> just = Flux.just(new Pojo("haroun", "tazieff"),
                new Pojo("haroun", "tazieff2"));
        Flux<Pojo> actual = httpClient()
                .compress(true)
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                .post()
                .uri("/yes")
                .send(codecManager.send("application/json", just, Pojo.class))
                .response(decodeAsFlux(Pojo.class));

        StepVerifier.create(actual)
                .expectNext(new Pojo("haroun", "tazieff from server"))
                .expectNext(new Pojo("haroun", "tazieff2 from server"))
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldReceiveNotFoundWhenNoRouteMatch() {
        StepVerifier.create(httpClient()
                .get()
                .uri("/yes_not_exists")
                .response(decodeAsFlux(String.class)))
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
                .response(decodeAsFlux(String.class)))
                .expectNext("oui")
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldFilterUnauthorized() {
        StepVerifier.create(httpClient()
                .get()
                .uri("/non")
                .response(decodeAsFlux(String.class)))
                .expectErrorMessage("401 : Unauthorized")
                .verify();
    }

    private <T> BiFunction<HttpClientResponse, ByteBufFlux, Mono<T>> decodeAsMono(Class<T> clazz) {
        return (response, flux) -> {
            HttpResponseStatus status = response.status();
            if (status.code() == 200) {
                return codecManager.decodeAsMono(clazz).apply(response, flux);
            } else {
                return Mono.error(new HttpException(status.code(), status.code() + " : " + status.reasonPhrase()));
            }
        };
    }

    private <T> BiFunction<HttpClientResponse, ByteBufFlux, Flux<T>> decodeAsFlux(Class<T> clazz) {
        return (response, flux) -> {
            HttpResponseStatus status = response.status();
            if (status.code() == 200) {
                return codecManager.decodeAsFlux(clazz).apply(response, flux);
            } else {
                return Flux.error(new HttpException(status.code(), status.code() + " : " + status.reasonPhrase()));
            }
        };
    }

    private HttpClient httpClient() {
        return HttpClient.create()
                .protocol(HttpProtocol.HTTP11)
                .baseUrl("http://localhost:" + tested.port())
                .headers(httpHeaders -> httpHeaders.set(HttpHeaderNames.ACCEPT, "application/json"));
    }


    private Body<byte[]> readFileAsFlux(String filename) {
        Flux<byte[]> flux = Flux.create(fluxSink -> {
            try {
                RandomAccessFile aFile = new RandomAccessFile(getClass().getResource(filename).getFile(), "r");
                FileChannel inChannel = aFile.getChannel();
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                while (inChannel.read(buffer) > 0) {
                    buffer.flip();
                    int len = buffer.limit();
                    byte[] bytes = new byte[len];
                    System.arraycopy(buffer.array(), buffer.arrayOffset(), bytes, 0, len);
                    buffer.clear(); // do something with the data and clear/compact it.
                    fluxSink.next(bytes);
                }
                inChannel.close();
                aFile.close();
                fluxSink.complete();
            } catch (IOException e) {
                fluxSink.error(e);
            }
        });
        return new Body<>(flux, TypeToken.of(byte[].class));
    }

    private Mono<byte[]> asBinary(Flux<byte[]> data) {
        return ByteBufFlux.fromInbound(data).aggregate().asInputStream()
                .map(inputStream -> {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    try {
                        IOUtils.copy(inputStream, out);
                    } catch (IOException e) {
                        throw Throwables.propagate(e);
                    }
                    return out.toByteArray();
                });
    }

    private Mono<String> asString(Flux<byte[]> data) {
        return asBinary(data).map(b -> new String(b, Charsets.UTF_8));
    }
}