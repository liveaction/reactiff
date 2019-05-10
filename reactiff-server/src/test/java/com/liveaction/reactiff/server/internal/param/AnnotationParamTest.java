package com.liveaction.reactiff.server.internal.param;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.server.mock.*;
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
    public void shouldParsePathParamWithPrimitiveBooleanType() {
        StepVerifier.create(
                withReactiveServer.httpClient()
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .get()
                        .uri("/annotated/params/primitive/boolean/true")
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
    public void shouldParsePathParamFromConstructor() {
        StepVerifier.create(
                withReactiveServer.httpClient()
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .get()
                        .uri("/annotated/params/constructor/test-pojo")
                        .response(withCodecManager.checkErrorAndDecodeAsMono(PojoWithConstructor.class)))
                .expectNext(new PojoWithConstructor("test-pojo"))
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldParsePathParamFromFromMethod() {
        StepVerifier.create(
                withReactiveServer.httpClient()
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .get()
                        .uri("/annotated/params/from/test-pojo")
                        .response(withCodecManager.checkErrorAndDecodeAsMono(PojoWithFrom.class)))
                .expectNext(PojoWithFrom.from("test-pojo"))
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldParsePathParamFromFromStringMethod() {
        StepVerifier.create(
                withReactiveServer.httpClient()
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .get()
                        .uri("/annotated/params/fromstring/test-pojo")
                        .response(withCodecManager.checkErrorAndDecodeAsMono(PojoWithFromString.class)))
                .expectNext(PojoWithFromString.fromString("test-pojo"))
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldParsePathParamFromValueOfMethod() {
        StepVerifier.create(
                withReactiveServer.httpClient()
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .get()
                        .uri("/annotated/params/valueof/test-pojo")
                        .response(withCodecManager.checkErrorAndDecodeAsMono(PojoWithValueOf.class)))
                .expectNext(PojoWithValueOf.valueOf("test-pojo"))
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

    @Test
    public void shouldParseParametrizedBodyParameter() {
        TypeToken<ImmutableList<Pojo>> type = new TypeToken<ImmutableList<Pojo>>() {
        };
        StepVerifier.create(
                withReactiveServer.httpClient()
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .post()
                        .uri("/annotated/body/parametrized")
                        .send(withCodecManager.codecManager.send("application/json", Flux.just(ImmutableList.of(new Pojo("haroun", "lebody")), ImmutableList.of(new Pojo("jean", "paul"))), type))
                        .response(withCodecManager.checkErrorAndDecodeAsFlux(Pojo.class)))
                .expectNext(new Pojo("haroun", "lebody"))
                .expectNext(new Pojo("jean", "paul"))
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldParseParametrizedBodyParameter2Level() {
        TypeToken<ImmutableList<ImmutableList<Pojo>>> type = new TypeToken<ImmutableList<ImmutableList<Pojo>>>() {
        };
        ImmutableList<ImmutableList<Pojo>> pojos1 = ImmutableList.of(
                ImmutableList.of(new Pojo("haroun", "lebody")),
                ImmutableList.of(new Pojo("jean", "paul")));

        ImmutableList<ImmutableList<Pojo>> pojos2 = ImmutableList.of(
                ImmutableList.of(new Pojo("alexandre", "legrand")));

        StepVerifier.create(
                withReactiveServer.httpClient()
                        .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                        .post()
                        .uri("/annotated/body/parametrized/2/level")
                        .send(withCodecManager.codecManager.send("application/json", Flux.just(pojos1, pojos2), type))
                        .response(withCodecManager.checkErrorAndDecodeAsFlux(Pojo.class)))
                .expectNext(new Pojo("haroun", "lebody"))
                .expectNext(new Pojo("jean", "paul"))
                .expectNext(new Pojo("alexandre", "legrand"))
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldParseHeaderParametersUsingValue1() {
        StepVerifier.create(
                withReactiveServer.httpClient()
                        .headers(httpHeaders -> httpHeaders.set("X-String", "My String"))
                        .get()
                        .uri("/annotated/headers/1")
                        .response(withCodecManager.checkErrorAndDecodeAsMono(String.class)))
                .expectNext("My String")
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldParseHeaderParametersUsingValue2() {
        StepVerifier.create(
                withReactiveServer.httpClient()
                        .headers(httpHeaders -> httpHeaders.set("X-Integer", "10"))
                        .get()
                        .uri("/annotated/headers/2")
                        .response(withCodecManager.checkErrorAndDecodeAsMono(Integer.class)))
                .expectNext(10)
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldParseHeaderParametersUsingValue3() {
        StepVerifier.create(
                withReactiveServer.httpClient()
                        .headers(httpHeaders -> httpHeaders.set("X-Double", "2.345"))
                        .get()
                        .uri("/annotated/headers/3")
                        .response(withCodecManager.checkErrorAndDecodeAsMono(Double.class)))
                .expectNext(2.345)
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldParseHeaderParametersUsingValue4() {
        StepVerifier.create(
                withReactiveServer.httpClient()
                        .headers(httpHeaders -> httpHeaders.set("X-Boolean", "true"))
                        .get()
                        .uri("/annotated/headers/4")
                        .response(withCodecManager.checkErrorAndDecodeAsMono(Boolean.class)))
                .expectNext(true)
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldGetUriParams() {
        StepVerifier.create(withReactiveServer.httpClient()
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                .get()
                .uri("/annotated/uriparam?param=test1&param=test2")
                .response(withCodecManager.checkErrorAndDecodeAsFlux(String.class)))
                .expectNext("test1")
                .expectNext("test2")
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldGetHeaderParams() {
        StepVerifier.create(withReactiveServer.httpClient()
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json")
                        .set("Param", ImmutableList.of("test1", "test2")))
                .get()
                .uri("/annotated/headerparam")
                .response(withCodecManager.checkErrorAndDecodeAsFlux(String.class)))
                .expectNext("test1")
                .expectNext("test2")
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldGetUriParamsDefaultValue() {
        StepVerifier.create(withReactiveServer.httpClient()
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                .get()
                .uri("/annotated/uriparam/default")
                .response(withCodecManager.checkErrorAndDecodeAsFlux(String.class)))
                .expectNext("defaultTest")
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldGetHeaderParamsDefaultValue() {
        StepVerifier.create(withReactiveServer.httpClient()
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                .get()
                .uri("/annotated/headerparam/default")
                .response(withCodecManager.checkErrorAndDecodeAsFlux(String.class)))
                .expectNext("defaultTest")
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldGetUriParamsOverDefault() {
        StepVerifier.create(withReactiveServer.httpClient()
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json"))
                .get()
                .uri("/annotated/uriparam/default?param=test1&param=test2")
                .response(withCodecManager.checkErrorAndDecodeAsFlux(String.class)))
                .expectNext("test1")
                .expectNext("test2")
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldGetHeaderParamsOverDefault() {
        StepVerifier.create(withReactiveServer.httpClient()
                .headers(httpHeaders -> httpHeaders.set("Accept", "application/json")
                        .set("Param", ImmutableList.of("test1", "test2")))
                .get()
                .uri("/annotated/headerparam/default")
                .response(withCodecManager.checkErrorAndDecodeAsFlux(String.class)))
                .expectNext("test1")
                .expectNext("test2")
                .expectComplete()
                .verify();
    }
}