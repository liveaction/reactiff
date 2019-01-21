package com.liveaction.reactiff.server.netty;

import com.liveaction.reactiff.server.netty.annotation.HttpMethod;
import com.liveaction.reactiff.server.netty.annotation.RequestMapping;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;

import java.util.NoSuchElementException;

public class CollectAuditHandler implements ReactiveHandler {

    @RequestMapping(path = "/yes/nosuch", method = HttpMethod.GET)
    public Publisher<String> noSuchElementException(HttpServerRequest httpServerRequest) {
        return Mono.error(new NoSuchElementException("Element untel not found"));
    }

    @RequestMapping(path = "/yes/unauthorized", method = HttpMethod.GET)
    public Publisher<String> unauthorizedException(HttpServerRequest httpServerRequest) {
        return Mono.error(new IllegalAccessException("Access forbidden by me"));
    }

    @RequestMapping(path = "/yes/{name}", rank = 1, method = HttpMethod.GET)
    public Publisher<String> yes(HttpServerRequest httpServerRequest) {
        return Flux.just("Hey " + httpServerRequest.param("name"), "Hey baby !");
    }

}
