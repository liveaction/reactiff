package com.liveaction.reactiff.server.param;

import com.liveaction.reactiff.server.mock.Pojo;
import com.liveaction.reactiff.server.rules.WithCodecManager;
import com.liveaction.reactiff.server.rules.WithReactiveServer;
import org.junit.ClassRule;
import org.junit.Test;
import reactor.core.publisher.Flux;
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
    public void shouldParsePathParamWithBooleanType() {
        StepVerifier.create(
                withReactiveServer.httpClient()
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .get()
                        .uri("/annotated/params/boolean/true")
                        .response(withCodecManager.checkErrorAndDecodeAsMono(Boolean.class)))
                .expectNext(true)
                .expectComplete()
                .verify();

        StepVerifier.create(
                withReactiveServer.httpClient()
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .get()
                        .uri("/annotated/params/boolean/false")
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

    @Test
    public void shouldParseBodyParameter() {
        StepVerifier.create(
                withReactiveServer.httpClient()
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .post()
                        .uri("/annotated/body")
                        .send(withCodecManager.codecManager.send("application/json", Flux.just(new Pojo("haroun", "lebody")), Pojo.class))
                        .response(withCodecManager.checkErrorAndDecodeAsFlux(Pojo.class)))
                .expectNext(new Pojo("haroun", "lebody from server"))
                .expectComplete()
                .verify();
    }

}