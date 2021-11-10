package com.liveaction.reactiff.server.general;

import com.google.common.collect.ImmutableSet;
import com.liveaction.reactiff.server.DefaultFilters;
import com.liveaction.reactiff.server.general.example.AuthFilter;
import com.liveaction.reactiff.server.general.example.InvalidRouteController;
import com.liveaction.reactiff.server.general.example.TestController;
import com.liveaction.reactiff.server.rules.WithCodecManager;
import com.liveaction.reactiff.server.rules.WithReactiveServer;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import reactor.test.StepVerifier;

import java.util.NoSuchElementException;

public final class ReactiveHttpServerWithErrorTest {

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

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
            .withHandler(new InvalidRouteController());

    @Before
    public void setUp() {
        withReactiveServer.withHandler(new TestController());
    }

    @Test
    public void shouldRegisterTestController() {
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
    public void shouldNotRegisterInvalidRouteNameController() {
        StepVerifier.create(withReactiveServer.httpClient()
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                .get()
                .uri("/route/valid/test")
                .response(withCodecManager.checkErrorAndDecodeAsMono(Boolean.class)))
                .expectErrorMessage("404 : '/route/valid/test' not found")
                .verify();

        StepVerifier.create(withReactiveServer.httpClient()
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                .get()
                .uri("/route/invalid/test")
                .response(withCodecManager.checkErrorAndDecodeAsMono(Boolean.class)))
                .expectErrorMessage("404 : '/route/invalid/test' not found")
                .verify();
    }
}