package com.liveaction.reactiff.server;

import com.liveaction.reactiff.server.example.AnnotationTestController;
import com.liveaction.reactiff.server.rules.WithCodecManager;
import com.liveaction.reactiff.server.rules.WithReactiveServer;
import com.liveaction.reactiff.server.utils.TestUtils;
import org.junit.ClassRule;
import org.junit.Test;
import reactor.test.StepVerifier;

public class AnnotationParamTest {

    @ClassRule
    public static WithCodecManager withCodecManager = new WithCodecManager();

    @ClassRule
    public static WithReactiveServer withReactiveServer = new WithReactiveServer(withCodecManager)
            .withHandler(new AnnotationTestController());

    @Test
    public void shouldParsePathParamAndReturnsTheValue() {
        StepVerifier.create(
                TestUtils.httpClient(withReactiveServer.server)
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .get()
                        .uri("/annotated/params/true")
                        .response(TestUtils.checkErrorAndDecodeAsMono(Boolean.class)))
                .expectNext(true)
                .expectComplete()
                .verify();

        StepVerifier.create(
                TestUtils.httpClient(withReactiveServer.server)
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .get()
                        .uri("/annotated/params/false")
                        .response(TestUtils.checkErrorAndDecodeAsMono(Boolean.class)))
                .expectNext(false)
                .expectComplete()
                .verify();
    }

}