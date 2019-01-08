package com.liveaction.reactiff.server.netty;

import com.liveaction.reactiff.server.netty.annotation.Get;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;

import java.util.NoSuchElementException;

public class CollectAuditHandler implements ReactiveHandler {

    @Get(path = "/yes/nosuch")
    public Publisher<String> noSuchElementException(HttpServerRequest httpServerRequest) {
        return Mono.error(new NoSuchElementException("Element untel not found"));
    }

    @Get(path = "/yes/unauthorized")
    public Publisher<String> unauthorizedException(HttpServerRequest httpServerRequest) {
        return Mono.error(new IllegalAccessException("Access forbidden by me"));
    }

    @Get(path = "/yes/{name}", rank = 1)
    public Publisher<String> yes(HttpServerRequest httpServerRequest) {
        return Flux.just("Hey " + httpServerRequest.param("name"), "Hey baby !");
    }

}
