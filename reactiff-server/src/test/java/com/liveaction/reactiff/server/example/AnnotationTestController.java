package com.liveaction.reactiff.server.example;

import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.server.HttpMethod;
import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.api.server.annotation.PathParam;
import com.liveaction.reactiff.api.server.annotation.RequestBody;
import com.liveaction.reactiff.api.server.annotation.RequestMapping;
import com.liveaction.reactiff.server.example.api.Pojo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class AnnotationTestController implements ReactiveHandler {

    @RequestMapping(method = HttpMethod.GET, path = "/annotated/params/{pathParam}")
    public Mono<Boolean> testPathParameter(@PathParam("pathParam") String value) {
        return Mono.just(Boolean.valueOf(value));
    }

    @RequestMapping(method = HttpMethod.POST, path = "/annotated/body")
    public Flux<Pojo> testBodyParameter(@RequestBody Flux<Pojo> pojos) {
        return pojos.map(pojo -> new Pojo(pojo.id, pojo.name + " from server"));
    }

}
