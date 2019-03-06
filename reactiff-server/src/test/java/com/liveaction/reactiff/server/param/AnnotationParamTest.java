package com.liveaction.reactiff.server.param;

import com.liveaction.reactiff.server.rules.WithCodecManager;
import com.liveaction.reactiff.server.rules.WithReactiveServer;
import org.junit.ClassRule;
import org.junit.Test;
import reactor.test.StepVerifier;

public final class AnnotationParamTest {

    @ClassRule
    public static WithCodecManager withCodecManager = new WithCodecManager();

    @ClassRule
    public static WithReactiveServer withReactiveServer = new WithReactiveServer(withCodecManager)
            .withHandler(new AnnotationTestController());

    @Test
    public void shouldParsePathParamAndReturnsTheValue() {
        StepVerifier.create(
                withReactiveServer.httpClient()
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .get()
                        .uri("/annotated/params/true")
                        .response(withCodecManager.checkErrorAndDecodeAsMono(Boolean.class)))
                .expectNext(true)
                .expectComplete()
                .verify();

        StepVerifier.create(
                withReactiveServer.httpClient()
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .get()
                        .uri("/annotated/params/false")
                        .response(withCodecManager.checkErrorAndDecodeAsMono(Boolean.class)))
                .expectNext(false)
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldInferParamName() {
        StepVerifier.create(
                withReactiveServer.httpClient()
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .get()
                        .uri("/annotated/infer-param-name/true")
                        .response(withCodecManager.checkErrorAndDecodeAsMono(Boolean.class)))
                .expectNext(true)
                .expectComplete()
                .verify();

        StepVerifier.create(
                withReactiveServer.httpClient()
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .get()
                        .uri("/annotated/infer-param-name/false")
                        .response(withCodecManager.checkErrorAndDecodeAsMono(Boolean.class)))
                .expectNext(false)
                .expectComplete()
                .verify();
    }

}