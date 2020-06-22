package com.liveaction.reactiff.server.general;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import com.google.common.net.HttpHeaders;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.Body;
import com.liveaction.reactiff.server.DefaultFilters;
import com.liveaction.reactiff.server.general.example.AuthFilter;
import com.liveaction.reactiff.server.general.example.FileTransferController;
import com.liveaction.reactiff.server.general.example.TestController;
import com.liveaction.reactiff.server.mock.Pojo;
import com.liveaction.reactiff.server.rules.HttpException;
import com.liveaction.reactiff.server.rules.ReactorUtils;
import com.liveaction.reactiff.server.rules.WithCodecManager;
import com.liveaction.reactiff.server.rules.WithReactiveServer;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.http.client.PrematureCloseException;
import reactor.test.StepVerifier;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class ReactiveHttpServerTest {

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @ClassRule
    public static WithCodecManager withCodecManager = new WithCodecManager();

    private Path tmpFolder;
    private FileTransferController fileTransferController;

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

    @Before
    public void setUp() throws Exception {
        tmpFolder = temporaryFolder.newFolder().toPath();
        fileTransferController = new FileTransferController(tmpFolder);
        withReactiveServer.withHandler(fileTransferController);
    }

    @After
    public void tearDown() throws Exception {
        withReactiveServer.removeHandler(fileTransferController);
    }

    @Test
    public void shouldDownloadFile() {
        StepVerifier.create(withReactiveServer.httpClient()
                .get()
                .uri("/download/file")
                .response()
                .map(HttpClientResponse::responseHeaders)
                .map(headers -> headers.entries().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))))
                .expectNextMatches(map ->
                        map.get(HttpHeaderNames.CONTENT_DISPOSITION.toString()).equals("attachment; filename=\"table.csv\"")
                                && map.get(HttpHeaderNames.TRANSFER_ENCODING.toString()).equals(HttpHeaderValues.CHUNKED.toString())
                                && !map.containsKey(HttpHeaders.CONTENT_LENGTH)).expectComplete()
                .verify();
    }

    @Test
    public void shouldDownloadPath() {
        StepVerifier.create(withReactiveServer.httpClient()
                .get()
                .uri("/download/path")
                .response()
                .map(HttpClientResponse::responseHeaders)
                .map(headers -> headers.entries().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))))
                .expectNextMatches(map ->
                        map.get(HttpHeaderNames.CONTENT_DISPOSITION.toString()).equals("attachment; filename=\"table.csv\"")
                                && map.get(HttpHeaderNames.TRANSFER_ENCODING.toString()).equals(HttpHeaderValues.CHUNKED.toString())
                                && !map.containsKey(HttpHeaders.CONTENT_LENGTH)).expectComplete()
                .verify();
    }

    @Test
    public void shouldReceiveStrings() {
        StepVerifier.create(withReactiveServer.httpClient()
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
        StepVerifier.create(withReactiveServer.httpClient()
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                .get()
                .uri("/boolean")
                .response(withCodecManager.checkErrorAndDecodeAsMono(Boolean.class)))
                .expectNext(true)
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldReceiveBooleans() {
        StepVerifier.create(withReactiveServer.httpClient()
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                .get()
                .uri("/booleans")
                .response(withCodecManager.checkErrorAndDecodeAsFlux(Boolean.class)))
                .expectNext(true)
                .expectNext(false)
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldCatchErrorWhenHandlerThrowAnException() {
        StepVerifier.create(withReactiveServer.httpClient()
                .get()
                .uri("/failed")
                .response(withCodecManager.checkErrorAndDecodeAsMono(String.class)))
                .expectErrorMessage("500 : Always fail")
                .verify();
    }

    @Test
    public void shouldReceiveNoSuchElementException() {
        StepVerifier.create(withReactiveServer.httpClient()
                .get()
                .uri("/yes/nosuch")
                .response(withCodecManager.checkErrorAndDecodeAsFlux(String.class)))
                .expectErrorMessage("404 : Not Found")
                .verify();
    }

    @Test
    public void shouldReceiveNoSuchElementExceptionFlux() {
        StepVerifier.create(withReactiveServer.httpClient()
                .get()
                .uri("/yes/nosuchflux")
                .response(withCodecManager.checkErrorAndDecodeAsFlux(String.class)))
                .expectErrorMessage("404 : Not Found")
                .verify();
    }

    @Test
    public void shouldReceiveException_during_flux_delayed() {
        StepVerifier.create(withReactiveServer.httpClient()
                .get()
                .uri("/yes/exception-flux-delay")
                .response(withCodecManager.checkErrorAndDecodeAsFlux(String.class)))
                .expectNext("a")
                .expectNext("b")
                .expectError(PrematureCloseException.class) // We do not propagate error from inner publisher yet
                .verify();
    }

    @Test
    public void shouldReceiveException_during_mono_delayed() {
        StepVerifier.create(withReactiveServer.httpClient()
                .get()
                .uri("/yes/exception-flux-delay")
                .response(withCodecManager.checkErrorAndDecodeAsFlux(String.class)))
                .expectNext("a")
                .expectNext("b")
                .expectError(PrematureCloseException.class) // We do not propagate error from inner publisher yet
                .verify();
    }

    @Test
    public void shouldReceiveException_during_mono() {
        StepVerifier.create(withReactiveServer.httpClient()
                .get()
                .uri("/yes/exception-mono")
                .response(withCodecManager.checkErrorAndDecodeAsFlux(String.class)))
                .expectErrorMessage("500 : Element untel not found") // We do not propagate error from flux yet
                .verify();
    }

    @Test
    public void shouldReceiveUnauthorized() {
        StepVerifier.create(withReactiveServer.httpClient()
                .get()
                .uri("/yes/unauthorized")
                .response(withCodecManager.checkErrorAndDecodeAsFlux(String.class)))
                .expectErrorMessage("401 : Unauthorized")
                .verify();
    }

    @Test
    public void shouldPostAndReceivePojoUsingStreamJson() {
        StepVerifier.create(
                withReactiveServer.httpClient()
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/stream+json"))
                        .post()
                        .uri("/yes")
                        .send(withCodecManager.codecManager.send("application/stream+json", Mono.just(new Pojo("haroun", "tazieff")), Pojo.class))
                        .response(withCodecManager.checkErrorAndDecodeAsFlux(Pojo.class)))
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
            return withReactiveServer.httpClient()
                    .headers(httpHeaders -> httpHeaders.set("Accept", "application/stream+json"))
                    .post()
                    .uri("/yes")
                    .send(withCodecManager.codecManager.send("application/stream+json", just, Pojo.class))
                    .response(withCodecManager.checkErrorAndDecodeAsFlux(Pojo.class));
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
                withReactiveServer.httpClient()
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .post()
                        .uri("/yes")
                        .send(withCodecManager.codecManager.send("application/json", Flux.just(new Pojo("haroun", "tazieff")), Pojo.class))
                        .response(withCodecManager.checkErrorAndDecodeAsFlux(Pojo.class)))
                .expectNext(new Pojo("haroun", "tazieff from server"))
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldPostAndReceivePojo_flux() {
        Flux<Pojo> just = Flux.just(new Pojo("haroun", "tazieff"),
                new Pojo("haroun", "tazieff2"));
        Flux<Pojo> actual = withReactiveServer.httpClient()
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                .post()
                .uri("/yes")
                .send(withCodecManager.codecManager.send("application/json", just, Pojo.class))
                .response(withCodecManager.checkErrorAndDecodeAsFlux(Pojo.class));

        StepVerifier.create(actual)
                .expectNext(new Pojo("haroun", "tazieff from server"))
                .expectNext(new Pojo("haroun", "tazieff2 from server"))
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldPostAndReceivePojoHeavy_json() {
        Flux<Pojo> actual = withReactiveServer.httpClient()
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                .post()
                .uri("/yes/heavy?count=1000")
                .response(withCodecManager.checkErrorAndDecodeAsFlux(Pojo.class));

        StepVerifier.create(actual.count())
                .expectNext(1000L)
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldPostAndReceivePojoHeavy_stream_json() {
        Flux<Pojo> actual = withReactiveServer.httpClient()
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/stream+json"))
                .post()
                .uri("/yes/heavy?count=1000")
                .response(withCodecManager.checkErrorAndDecodeAsFlux(Pojo.class));

        StepVerifier.create(actual.count())
                .expectNext(1000L)
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldPostAndReceivePojoHeavy_binary() {
        Flux<Pojo> actual = withReactiveServer.httpClient()
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/octet-stream"))
                .post()
                .uri("/yes/heavy?count=1000")
                .response(withCodecManager.checkErrorAndDecodeAsFlux(Pojo.class));

        StepVerifier.create(actual.count())
                .expectNext(1000L)
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldPostAndReceivePojo_flux_binary() {
        Flux<Pojo> just = Flux.just(new Pojo("haroun", "tazieff"),
                new Pojo("haroun", "tazieff2"));
        Flux<Pojo> actual = withReactiveServer.httpClient()
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/octet-stream"))
                .post()
                .uri("/yes")
                .send(withCodecManager.codecManager.send("application/octet-stream", just, Pojo.class))
                .response(withCodecManager.checkErrorAndDecodeAsFlux(Pojo.class));

        StepVerifier.create(actual)
                .expectNext(new Pojo("haroun", "tazieff from server"))
                .expectNext(new Pojo("haroun", "tazieff2 from server"))
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldPostAndReceiveXMLFileAsByteArray() {
        Body<byte[]> body = ReactorUtils.readFileAsFlux("/test-xml-file.xml");
        Flux<byte[]> actual = withReactiveServer.httpClient()
                .post()
                .uri("/upload")
                .send(withCodecManager.codecManager.send("text/xml", body))
                .response(withCodecManager.checkErrorAndDecodeAsFlux(byte[].class));

        File expected = new File(getClass().getResource("/test-xml-file.xml").getFile());
        StepVerifier.create(ReactorUtils.asString(actual))
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
        Body<byte[]> body = ReactorUtils.readFileAsFlux("/sample.pdf");
        Flux<byte[]> actual = withReactiveServer.httpClient()
                .post()
                .uri("/upload")
                .send(withCodecManager.codecManager.send("application/pdf", body))
                .response(withCodecManager.checkErrorAndDecodeAsFlux(byte[].class));

        File expected = new File(getClass().getResource("/sample.pdf").getFile());
        StepVerifier.create(ReactorUtils.asBinary(actual))
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
        Body<File> body = ReactorUtils.readFile("/sample.pdf");
        Mono<File> actual = Mono.from(
                withReactiveServer.httpClient()
                        .compress(true)
                        .post()
                        .uri("/upload")
                        .send(withCodecManager.codecManager.send("application/pdf", body))
                        .response(withCodecManager.checkErrorAndDecodeAsMono(File.class))
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
        Flux<Pojo> actual = withReactiveServer.httpClient()
                .compress(true)
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                .post()
                .uri("/yes")
                .send(withCodecManager.codecManager.send("application/json", just, Pojo.class))
                .response(withCodecManager.checkErrorAndDecodeAsFlux(Pojo.class));

        StepVerifier.create(actual)
                .expectNext(new Pojo("haroun", "tazieff from server"))
                .expectNext(new Pojo("haroun", "tazieff2 from server"))
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldPostAndReceiveEmptyFlux() {
        Flux<Pojo> just = Flux.just();
        Flux<Pojo> actual = withReactiveServer.httpClient()
                .compress(true)
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                .post()
                .uri("/yes")
                .send(withCodecManager.codecManager.send("application/json", just, Pojo.class))
                .response(withCodecManager.checkErrorAndDecodeAsFlux(Pojo.class));

        StepVerifier.create(actual)
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldPostAndReceiveEmptyFluxAsJsonStream() {
        Flux<Pojo> just = Flux.just();
        Flux<Pojo> actual = withReactiveServer.httpClient()
                .compress(true)
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/stream+json"))
                .post()
                .uri("/yes")
                .send(withCodecManager.codecManager.send("application/stream+json", just, Pojo.class))
                .response(withCodecManager.checkErrorAndDecodeAsFlux(Pojo.class));

        StepVerifier.create(actual)
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldReceiveNotFoundWhenNoRouteMatch() {
        StepVerifier.create(withReactiveServer.httpClient()
                .get()
                .uri("/yes_not_exists")
                .response(withCodecManager.checkErrorAndDecodeAsFlux(String.class)))
                .expectErrorMessage("404 : '/yes_not_exists' not found")
                .verify();
    }

    @Test
    public void shouldListAllRoutesWhenNoRouteMatch() throws IOException {
        StepVerifier.create(withReactiveServer.httpClient()
                .headers(httpHeaders -> httpHeaders.set(HttpHeaderNames.ACCEPT, "text/plain"))
                .get()
                .uri("/yes_not_exists")
                .responseSingle((httpClientResponse, byteBufFlux) -> {
                    assertThat(httpClientResponse.status().code()).isEqualTo(404);
                    return withCodecManager.codecManager.decodeAsMono(String.class).apply(httpClientResponse, byteBufFlux);
                })
        ).expectNext(Files.toString(new File(getClass().getResource("/expected/not-found.txt").getFile()), Charsets.UTF_8))
                .verifyComplete();
    }

    @Test
    public void shouldHandlePreflightCORSRequest() {
        StepVerifier.create(withReactiveServer.httpClient()
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
                    return "http://localhost" .equals(origin) &&
                            "Accept,Accept-Language,Content-Language,Content-Type,X-UserToken" .equals(headers) &&
                            "DELETE,POST,GET,PUT" .equals(methods);
                })
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldHandleWebSocket() {
        Flux<WebSocketFrame> frames = withReactiveServer.httpClient()
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
    public void shouldHandleWebSocketAuth() {
        Flux<WebSocketFrame> frames = withReactiveServer.httpClient()
                .baseUrl("ws://localhost:" + withReactiveServer.server.port())
                .websocket()
                .uri("/websocket-auth")
                .handle((websocketInbound, websocketOutbound) -> websocketInbound.receiveFrames());

        StepVerifier.create(frames)
                .expectErrorMessage("Invalid handshake response getStatus: 401 Unauthorized")
                .verify();
    }

    @Test
    public void shouldNotFilterAuthorized() {
        StepVerifier.create(withReactiveServer.httpClient()
                .get()
                .uri("/oui")
                .response(withCodecManager.checkErrorAndDecodeAsFlux(String.class)))
                .expectNext("oui")
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldFilterUnauthorized() {
        StepVerifier.create(withReactiveServer.httpClient()
                .get()
                .uri("/non")
                .response(withCodecManager.checkErrorAndDecodeAsFlux(String.class)))
                .expectErrorMessage("401 : Unauthorized")
                .verify();
    }

    @Test
    public void shouldReceiveMonoFromFlux() {
        StepVerifier.create(
                withReactiveServer.httpClient()
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .post()
                        .uri("/boolean/mono/from/flux")
                        .send(withCodecManager.codecManager.send("application/json", Flux.just(false), Boolean.class))
                        .response(withCodecManager.checkErrorAndDecodeAsMono(Boolean.class)))
                .expectNext(true)
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldReceiveFluxFromMono() {
        StepVerifier.create(
                withReactiveServer.httpClient()
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .post()
                        .uri("/boolean/flux/from/mono")
                        .send(withCodecManager.codecManager.send("application/json", Mono.just(false), Boolean.class))
                        .response(withCodecManager.checkErrorAndDecodeAsFlux(Boolean.class)))
                .expectNext(true)
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldHandleVoid() {
        StepVerifier.create(
                withReactiveServer.httpClient()
                        .post()
                        .uri("/void")
                        .response(withCodecManager.checkErrorAndDecodeAsMono(Void.class)))
                .expectComplete()
                .verify();

        StepVerifier.create(
                withReactiveServer.httpClient()
                        .post()
                        .uri("/void?error=true")
                        .response(withCodecManager.checkErrorAndDecodeAsMono(Void.class)))
                .expectError(HttpException.class)
                .verify();
    }

    @Test
    public void shouldHandleMonoVoid() {
        StepVerifier.create(
                withReactiveServer.httpClient()
                        .post()
                        .uri("/monovoid")
                        .response(withCodecManager.checkErrorAndDecodeAsMono(Void.class)))
                .expectComplete()
                .verify();

    }

    @Test
    public void shouldSetCookie() {
        StepVerifier.create(
                withReactiveServer.httpClient()
                        .get()
                        .uri("/setCookie")
                        .response())
                .expectNextMatches(httpClientResponse -> {
                    if (httpClientResponse.cookies().size() == 1) {
                        Cookie cookie = httpClientResponse.cookies().get("cookieName").iterator().next();
                        return cookie.isSecure() == true
                                && cookie.isHttpOnly() == true
                                && cookie.value().equals("cookieValue")
                                && cookie.maxAge() == Duration.ofHours(1).getSeconds();
                    } else {
                        return false;
                    }
                })
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldPostMultiPart_fields() {
        StepVerifier.create(withReactiveServer.httpClient()
                .post()
                .uri("/multipart")
                .sendForm((req, form) -> form.multipart(true)
                        .file("test", new ByteArrayInputStream("test file" .getBytes()))
                        .attr("att1", "val1")
                        .attr("att2", "val2")
                        .file("test2", new ByteArrayInputStream("test file 2" .getBytes())))
                .response(withCodecManager.checkErrorAndDecodeAsMono(new TypeToken<Map<String, String>>() {
                }))
                .map(ImmutableMap::copyOf))
                .expectNext(ImmutableMap.of("att1", "val1", "att2", "val2"))
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldPostMultiPart_files() throws IOException {
        StepVerifier.create(withReactiveServer.httpClient()
                .post()
                .uri("/upload/multipart")
                .sendForm((req, form) -> form.multipart(true)
                        .file("test", "file1", new ByteArrayInputStream("test file" .getBytes()), null)
                        .attr("att1", "val1")
                        .attr("att2", "val2")
                        .file("test2", "file2", new ByteArrayInputStream("test file 2" .getBytes()), null))
                .response(withCodecManager.checkErrorAndDecodeAsFlux(String.class)))
                .expectNext(tmpFolder.resolve("file1").toString())
                .expectNext(tmpFolder.resolve("file2").toString())
                .expectComplete()
                .verify();
        assertThat(Files.readFirstLine(tmpFolder.resolve("file1").toFile(), Charset.defaultCharset())).isEqualTo("test file");
        assertThat(Files.readFirstLine(tmpFolder.resolve("file2").toFile(), Charset.defaultCharset())).isEqualTo("test file 2");
    }
}