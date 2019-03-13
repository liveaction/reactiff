package com.liveaction.reactiff.server.param;

import com.liveaction.reactiff.api.server.HttpMethod;
import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.api.server.annotation.HeaderParam;
import com.liveaction.reactiff.api.server.annotation.PathParam;
import com.liveaction.reactiff.api.server.annotation.RequestBody;
import com.liveaction.reactiff.api.server.annotation.RequestMapping;
import com.liveaction.reactiff.server.mock.Pojo;
import com.liveaction.reactiff.server.mock.PojoWithConstructor;
import com.liveaction.reactiff.server.mock.PojoWithFrom;
import com.liveaction.reactiff.server.mock.PojoWithFromString;
import com.liveaction.reactiff.server.mock.PojoWithValueOf;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public final class AnnotationTestController implements ReactiveHandler {

    @RequestMapping(method = HttpMethod.GET, path = "/annotated/params/{pathParam}")
    public Mono<Boolean> testPathParameter(@PathParam("pathParam") String value) {
        return Mono.just(Boolean.valueOf(value));
    }

    @RequestMapping(method = HttpMethod.GET, path = "/annotated/params/boolean/{pathParam}")
    public Mono<Boolean> testPathParameterAsBoolean(@PathParam("pathParam") Boolean value) {
        return Mono.just(value);
    }

    @RequestMapping(method = HttpMethod.GET, path = "/annotated/params/primitive/boolean/{pathParam}")
    public Mono<Boolean> testPathParameterAsPrimitiveBoolean(@PathParam("pathParam") boolean value) {
        return Mono.just(value);
    }

    @RequestMapping(method = HttpMethod.GET, path = "/annotated/params/constructor/{pathParam}")
    public Mono<PojoWithConstructor> testPathParameterConstructor(@PathParam("pathParam") PojoWithConstructor pojo) {
        return Mono.just(pojo);
    }

    @RequestMapping(method = HttpMethod.GET, path = "/annotated/params/from/{pathParam}")
    public Mono<PojoWithFrom> testPathParameterFrom(@PathParam("pathParam") PojoWithFrom pojo) {
        return Mono.just(pojo);
    }

    @RequestMapping(method = HttpMethod.GET, path = "/annotated/params/fromstring/{pathParam}")
    public Mono<PojoWithFromString> testPathParameterFromString(@PathParam("pathParam") PojoWithFromString pojo) {
        return Mono.just(pojo);
    }

    @RequestMapping(method = HttpMethod.GET, path = "/annotated/params/valueof/{pathParam}")
    public Mono<PojoWithValueOf> testPathParameterValueOf(@PathParam("pathParam") PojoWithValueOf pojo) {
        return Mono.just(pojo);
    }

    @RequestMapping(method = HttpMethod.GET, path = "/annotated/infer-param-name/{pathParam}")
    public Mono<Boolean> testPathParameterInferName(@PathParam String pathParam) {
        return Mono.just(Boolean.valueOf(pathParam));
    }

    @RequestMapping(method = HttpMethod.POST, path = "/annotated/body")
    public Flux<Pojo> testBodyParameter(@RequestBody Flux<Pojo> pojos) {
        return pojos.map(pojo -> new Pojo(pojo.id, pojo.name + " from server"));
    }

    @RequestMapping(method = HttpMethod.POST, path = "/annotated/body/parametrized")
    public Flux<Pojo> testParametrizedBodyParameter(@RequestBody Flux<List<Pojo>> pojoLists) {
        return pojoLists.flatMapIterable(pojos -> pojos);
    }

    @RequestMapping(method = HttpMethod.POST, path = "/annotated/body/parametrized/2/level")
    public Flux<Pojo> testParametrizedBodyParameter2Level(@RequestBody Flux<List<List<Pojo>>> pojoLists) {
        return pojoLists.flatMap(pojos -> Flux.fromIterable(pojos).flatMapIterable(p -> p));
    }

    @RequestMapping(method = HttpMethod.GET, path = "/annotated/headers/1")
    public Mono<String> testParametrizedHeaderParametersUsingValue1(@HeaderParam(value = "X-String") String xString) {
        return Mono.just(xString);
    }

    @RequestMapping(method = HttpMethod.GET, path = "/annotated/headers/2")
    public Mono<Integer> testParametrizedHeaderParametersUsingValue2(@HeaderParam(value = "X-Integer") Integer xInteger) {
        return Mono.just(xInteger);
    }

    @RequestMapping(method = HttpMethod.GET, path = "/annotated/headers/3")
    public Mono<Double> testParametrizedHeaderParametersUsingValue3(@HeaderParam(value = "X-Double") Double xDouble) {
        return Mono.just(xDouble);
    }

    @RequestMapping(method = HttpMethod.GET, path = "/annotated/headers/4")
    public Mono<Boolean> testParametrizedHeaderParametersUsingValue4(@HeaderParam(value = "X-Boolean") Boolean xBoolean) {
        return Mono.just(xBoolean);
    }

}
