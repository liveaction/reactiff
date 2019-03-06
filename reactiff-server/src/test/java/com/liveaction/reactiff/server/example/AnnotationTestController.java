package com.liveaction.reactiff.server.example;

import com.liveaction.reactiff.api.server.HttpMethod;
import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.api.server.annotation.PathParam;
import com.liveaction.reactiff.api.server.annotation.RequestMapping;
import reactor.core.publisher.Mono;

public class AnnotationTestController implements ReactiveHandler {

    @RequestMapping(method = HttpMethod.GET, path = "/annotated/params/{pathParam}")
    public Mono<Boolean> testPathParameter(@PathParam("pathParam") String value) {
        return Mono.just(Boolean.valueOf(value));
    }

}
