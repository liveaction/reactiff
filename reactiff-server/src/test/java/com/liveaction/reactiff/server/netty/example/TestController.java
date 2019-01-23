package com.liveaction.reactiff.server.netty.example;

import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.server.netty.example.api.Pojo;
import com.liveaction.reactiff.server.netty.ReactiveHandler;
import com.liveaction.reactiff.server.netty.Request;
import com.liveaction.reactiff.server.netty.annotation.RequestMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

import static com.liveaction.reactiff.server.netty.HttpMethod.GET;
import static com.liveaction.reactiff.server.netty.HttpMethod.POST;

public class TestController implements ReactiveHandler {

    @RequestMapping(method = GET, path = "/yes/nosuch")
    public Mono<Void> noSuchElementException(Request request) {
        return Mono.error(new NoSuchElementException("Element untel not found"));
    }

    @RequestMapping(method = GET, path = "/yes/unauthorized")
    public Mono<Void> unauthorizedException(Request request) {
        return Mono.error(new IllegalAccessException("Access forbidden by me"));
    }

    @RequestMapping(method = GET, path = "/yes/{name}", rank = 1)
    public Flux<String> yes(Request request) {
        return Flux.just("Hey " + request.pathParam("name"), "Hey baby !");
    }

    @RequestMapping(method = POST, path = "/yes")
    public Mono<Pojo> postPojo(Request request) {
        return request.bodyToMono(new TypeToken<Pojo>() {
        })
                .map(pojo -> new Pojo(pojo.id, pojo.name + " from server"));
    }

}
