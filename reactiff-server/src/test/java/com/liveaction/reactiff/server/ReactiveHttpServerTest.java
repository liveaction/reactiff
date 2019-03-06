package com.liveaction.reactiff.server;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.Body;
import com.liveaction.reactiff.server.example.AuthFilter;
import com.liveaction.reactiff.server.example.TestController;
import com.liveaction.reactiff.server.example.api.Pojo;
import com.liveaction.reactiff.server.rules.WithCodecManager;
import com.liveaction.reactiff.server.rules.WithReactiveServer;
import com.liveaction.reactiff.server.utils.TestUtils;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.junit.ClassRule;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.time.Duration;
import java.util.NoSuchElementException;

import static com.liveaction.reactiff.server.utils.TestUtils.asBinary;
import static com.liveaction.reactiff.server.utils.TestUtils.asString;
import static com.liveaction.reactiff.server.utils.TestUtils.checkErrorAndDecodeAsMono;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ReactiveHttpServerTest {

    @ClassRule
    public static WithCodecManager withCodecManager = new WithCodecManager();

    @ClassRule
    public static WithReactiveServer withReactiveServer = new WithReactiveServer(withCodecManager)
            .withFilter(DefaultFilters.cors(
                    ImmutableSet.of("http://localhost"),
                    ImmutableSet.of("X-UserToken"),
                    ImmutableSet.of("GET", "POST", "PUT", "DELETE"),
                    false,
                    -1
            ))
            .withFilter(DefaultFilters.exceptionMapping(throwable -> {
                if (throwable instanceof IllegalAccessException) {
                    return 401;
                } else if (throwable instanceof NoSuchElementException) {
                    return 404;
                } else {
                    return null;
                }
            }))
            .withFilter(new AuthFilter())
            .withHandler(new TestController());

    @Test
    public void shouldReceiveStrings() {
        StepVerifier.create(TestUtils.httpClient(withReactiveServer.server)
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
    public void shouldReceiveBoolean() {
        StepVerifier.create(TestUtils.httpClient(withReactiveServer.server)
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                .get()
                .uri("/boolean")
                .response(checkErrorAndDecodeAsMono(Boolean.class)))
                .expectNext(true)
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldReceiveBooleans() {
        StepVerifier.create(TestUtils.httpClient(withReactiveServer.server)
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                .get()
                .uri("/booleans")
                .response(TestUtils.checkErrorAndDecodeAsFlux(Boolean.class)))
                .expectNext(true)
                .expectNext(false)
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldCatchErrorWhenHandlerThrowAnException() {
        StepVerifier.create(TestUtils.httpClient(withReactiveServer.server)
                .get()
                .uri("/failed")
                .response(checkErrorAndDecodeAsMono(String.class)))
                .expectErrorMessage("500 : Internal Server Error")
                .verify();
    }

    @Test
    public void shouldReceiveNoSuchElementException() {
        StepVerifier.create(TestUtils.httpClient(withReactiveServer.server)
                .get()
                .uri("/yes/nosuch")
                .response(TestUtils.checkErrorAndDecodeAsFlux(String.class)))
                .expectErrorMessage("404 : Not Found")
                .verify();
    }

    @Test
    public void shouldReceiveNoSuchElementExceptionFlux() {
        StepVerifier.create(TestUtils.httpClient(withReactiveServer.server)
                .get()
                .uri("/yes/nosuchflux")
                .response(TestUtils.checkErrorAndDecodeAsFlux(String.class)))
                .expectErrorMessage("404 : Not Found")
                .verify();
    }

    @Test
    public void shouldReceiveUnauthorized() {
        StepVerifier.create(TestUtils.httpClient(withReactiveServer.server)
                .get()
                .uri("/yes/unauthorized")
                .response(TestUtils.checkErrorAndDecodeAsFlux(String.class)))
                .expectErrorMessage("401 : Unauthorized")
                .verify();
    }

    @Test
    public void shouldPostAndReceivePojoUsingStreamJson() {
        StepVerifier.create(
                TestUtils.httpClient(withReactiveServer.server)
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/stream+json"))
                        .post()
                        .uri("/yes")
                        .send(withCodecManager.codecManager.send("application/stream+json", Mono.just(new Pojo("haroun", "tazieff")), Pojo.class))
                        .response(TestUtils.checkErrorAndDecodeAsFlux(Pojo.class)))
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
            return TestUtils.httpClient(withReactiveServer.server)
                    .headers(httpHeaders -> httpHeaders.set("Accept", "application/stream+json"))
                    .post()
                    .uri("/yes")
                    .send(withCodecManager.codecManager.send("application/stream+json", just, Pojo.class))
                    .response(TestUtils.checkErrorAndDecodeAsFlux(Pojo.class));
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
                TestUtils.httpClient(withReactiveServer.server)
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .post()
                        .uri("/yes")
                        .send(withCodecManager.codecManager.send("application/json", Flux.just(new Pojo("haroun", "tazieff")), Pojo.class))
                        .response(TestUtils.checkErrorAndDecodeAsFlux(Pojo.class)))
                .expectNext(new Pojo("haroun", "tazieff from server"))
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldPostAndReceivePojo_flux() {
        Flux<Pojo> just = Flux.just(new Pojo("haroun", "tazieff"),
                new Pojo("haroun", "tazieff2"));
        Flux<Pojo> actual = TestUtils.httpClient(withReactiveServer.server)
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                .post()
                .uri("/yes")
                .send(withCodecManager.codecManager.send("application/json", just, Pojo.class))
                .response(TestUtils.checkErrorAndDecodeAsFlux(Pojo.class));

        StepVerifier.create(actual)
                .expectNext(new Pojo("haroun", "tazieff from server"))
                .expectNext(new Pojo("haroun", "tazieff2 from server"))
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldPostAndReceivePojoHeavy_json() {
        Flux<Pojo> actual = TestUtils.httpClient(withReactiveServer.server)
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                .post()
                .uri("/yes/heavy?count=1000")
                .response(TestUtils.checkErrorAndDecodeAsFlux(Pojo.class));

        StepVerifier.create(actual.count())
                .expectNext(1000L)
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldPostAndReceivePojoHeavy_stream_json() {
        Flux<Pojo> actual = TestUtils.httpClient(withReactiveServer.server)
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/stream+json"))
                .post()
                .uri("/yes/heavy?count=1000")
                .response(TestUtils.checkErrorAndDecodeAsFlux(Pojo.class));

        StepVerifier.create(actual.count())
                .expectNext(1000L)
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldPostAndReceivePojoHeavy_binary() {
        Flux<Pojo> actual = TestUtils.httpClient(withReactiveServer.server)
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/octet-stream"))
                .post()
                .uri("/yes/heavy?count=1000")
                .response(TestUtils.checkErrorAndDecodeAsFlux(Pojo.class));

        StepVerifier.create(actual.count())
                .expectNext(1000L)
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldPostAndReceivePojo_flux_binary() {
        Flux<Pojo> just = Flux.just(new Pojo("haroun", "tazieff"),
                new Pojo("haroun", "tazieff2"));
        Flux<Pojo> actual = TestUtils.httpClient(withReactiveServer.server)
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/octet-stream"))
                .post()
                .uri("/yes")
                .send(withCodecManager.codecManager.send("application/octet-stream", just, Pojo.class))
                .response(TestUtils.checkErrorAndDecodeAsFlux(Pojo.class));

        StepVerifier.create(actual)
                .expectNext(new Pojo("haroun", "tazieff from server"))
                .expectNext(new Pojo("haroun", "tazieff2 from server"))
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldPostAndReceiveXMLFileAsByteArray() {
        Body<byte[]> body = readFileAsFlux("/test-xml-file.xml");
        Flux<byte[]> actual = TestUtils.httpClient(withReactiveServer.server)
                .post()
                .uri("/upload")
                .send(withCodecManager.codecManager.send("text/xml", body))
                .response(TestUtils.checkErrorAndDecodeAsFlux(byte[].class));

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
    public void shouldPostAndReceiveBinaryFileAsByteArray() {
        Body<byte[]> body = readFileAsFlux("/sample.pdf");
        Flux<byte[]> actual = TestUtils.httpClient(withReactiveServer.server)
                .post()
                .uri("/upload")
                .send(withCodecManager.codecManager.send("application/pdf", body))
                .response(TestUtils.checkErrorAndDecodeAsFlux(byte[].class));

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
    public void shouldPostAndReceiveBinaryFile() {
        Body<File> body = readFile("/sample.pdf");
        Mono<File> actual = Mono.from(
                TestUtils.httpClient(withReactiveServer.server)
                        .compress(true)
                        .post()
                        .uri("/upload")
                        .send(withCodecManager.codecManager.send("application/pdf", body))
                        .response(checkErrorAndDecodeAsMono(File.class))
        );

        File expected = new File(getClass().getResource("/sample.pdf").getFile());
        StepVerifier.create(actual)
                .assertNext(content -> {
                    try {
                        assertThat(content).hasBinaryContent(Files.toByteArray(expected));
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
        Flux<Pojo> actual = TestUtils.httpClient(withReactiveServer.server)
                .compress(true)
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                .post()
                .uri("/yes")
                .send(withCodecManager.codecManager.send("application/json", just, Pojo.class))
                .response(TestUtils.checkErrorAndDecodeAsFlux(Pojo.class));

        StepVerifier.create(actual)
                .expectNext(new Pojo("haroun", "tazieff from server"))
                .expectNext(new Pojo("haroun", "tazieff2 from server"))
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldPostAndReceiveEmptyFlux() {
        Flux<Pojo> just = Flux.just();
        Flux<Pojo> actual = TestUtils.httpClient(withReactiveServer.server)
                .compress(true)
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                .post()
                .uri("/yes")
                .send(withCodecManager.codecManager.send("application/json", just, Pojo.class))
                .response(TestUtils.checkErrorAndDecodeAsFlux(Pojo.class));

        StepVerifier.create(actual)
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldPostAndReceiveEmptyFluxAsJsonStream() {
        Flux<Pojo> just = Flux.just();
        Flux<Pojo> actual = TestUtils.httpClient(withReactiveServer.server)
                .compress(true)
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/stream+json"))
                .post()
                .uri("/yes")
                .send(withCodecManager.codecManager.send("application/stream+json", just, Pojo.class))
                .response(TestUtils.checkErrorAndDecodeAsFlux(Pojo.class));

        StepVerifier.create(actual)
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldReceiveNotFoundWhenNoRouteMatch() {
        StepVerifier.create(TestUtils.httpClient(withReactiveServer.server)
                .get()
                .uri("/yes_not_exists")
                .response(TestUtils.checkErrorAndDecodeAsFlux(String.class)))
                .expectErrorMessage("404 : Not Found")
                .verify();
    }

    @Test
    public void shouldListAllRoutesWhenNoRouteMatch() throws IOException {
        String actual = TestUtils.httpClient(withReactiveServer.server)
                .headers(httpHeaders -> httpHeaders.set(HttpHeaderNames.ACCEPT, "text/plain"))
                .get()
                .uri("/yes_not_exists")
                .responseSingle((httpClientResponse, byteBufFlux) -> {
                    assertThat(httpClientResponse.status().code()).isEqualTo(404);
                    return withCodecManager.codecManager.decodeAsMono(String.class).apply(httpClientResponse, byteBufFlux);
                })
                .block();
        assertThat(actual).isEqualTo(Files.toString(new File(getClass().getResource("/expected/not-found.txt").getFile()), Charsets.UTF_8));
    }

    @Test
    public void shouldHandlePreflightCORSRequest() {
        StepVerifier.create(TestUtils.httpClient(withReactiveServer.server)
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
        Flux<WebSocketFrame> frames = TestUtils.httpClient(withReactiveServer.server)
                .baseUrl("ws://localhost:" + withReactiveServer.server.port())
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
        StepVerifier.create(TestUtils.httpClient(withReactiveServer.server)
                .get()
                .uri("/oui")
                .response(TestUtils.checkErrorAndDecodeAsFlux(String.class)))
                .expectNext("oui")
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldFilterUnauthorized() {
        StepVerifier.create(TestUtils.httpClient(withReactiveServer.server)
                .get()
                .uri("/non")
                .response(TestUtils.checkErrorAndDecodeAsFlux(String.class)))
                .expectErrorMessage("401 : Unauthorized")
                .verify();
    }

    @Test
    public void shouldReceiveMonoFromFlux() {
        StepVerifier.create(
                TestUtils.httpClient(withReactiveServer.server)
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .post()
                        .uri("/boolean/mono/from/flux")
                        .send(withCodecManager.codecManager.send("application/json", Flux.just(false), Boolean.class))
                        .response(checkErrorAndDecodeAsMono(Boolean.class)))
                .expectNext(true)
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldReceiveFluxFromMono() {
        StepVerifier.create(
                TestUtils.httpClient(withReactiveServer.server)
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .post()
                        .uri("/boolean/flux/from/mono")
                        .send(withCodecManager.codecManager.send("application/json", Mono.just(false), Boolean.class))
                        .response(TestUtils.checkErrorAndDecodeAsFlux(Boolean.class)))
                .expectNext(true)
                .expectComplete()
                .verify();
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

    private Body<File> readFile(String fileName) {
        return new Body<>(Mono.just(new File(getClass().getResource(fileName).getFile())), TypeToken.of(File.class));
    }


}