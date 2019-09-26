package com.liveaction.reactiff.server.general;

import com.google.common.collect.ImmutableSet;
import com.liveaction.reactiff.server.DefaultFilters;
import com.liveaction.reactiff.server.general.example.AuthFilter;
import com.liveaction.reactiff.server.general.example.ConflictController;
import com.liveaction.reactiff.server.rules.WithCodecManager;
import com.liveaction.reactiff.server.rules.WithReactiveServer;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.junit.ClassRule;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.NoSuchElementException;

public final class ConflictServerTest {

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
            .withHandler(new ConflictController());

    @Test
    public void shouldReceiveConflictTest() {
        StepVerifier.create(withReactiveServer.httpClient()
                .headers(httpHeaders -> httpHeaders.set("Accept", "text/plain"))
                .get()
                .uri("/conflict/test")
                .responseContent()
                .asString())
                .expectNext("This is a test !")
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldHandlerWebSocketConflict() {
        Flux<WebSocketFrame> frames = withReactiveServer.httpClient()
                .baseUrl("ws://localhost:" + withReactiveServer.server.port())
                .websocket()
                .uri("/conflict/ws")
                .handle((websocketInbound, websocketOutbound) -> websocketInbound.receiveFrames());

        StepVerifier.create(frames)
                .expectNext(new TextWebSocketFrame("Salut !"))
                .expectNext(new TextWebSocketFrame("Je m'appelle"))
                .expectNext(new TextWebSocketFrame("Jean Baptiste Poquelin"))
                .expectComplete()
                .verify();
    }

}