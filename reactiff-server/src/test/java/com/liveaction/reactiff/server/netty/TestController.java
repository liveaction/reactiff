package com.liveaction.reactiff.server.netty;

import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.codec.CodecManager;
import com.liveaction.reactiff.server.netty.annotation.RequestMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;

import java.util.NoSuchElementException;

import static com.liveaction.reactiff.server.netty.annotation.HttpMethod.GET;
import static com.liveaction.reactiff.server.netty.annotation.HttpMethod.POST;

public class TestController implements ReactiveHandler {

    private CodecManager codecManager;

    public void setCodecManager(CodecManager codecManager) {
        this.codecManager = codecManager;
    }

    @RequestMapping(method = GET, path = "/yes/nosuch")
    public Mono<Void> noSuchElementException(HttpServerRequest httpServerRequest) {
        return Mono.error(new NoSuchElementException("Element untel not found"));
    }

    @RequestMapping(method = GET, path = "/yes/unauthorized")
    public Mono<Void> unauthorizedException(HttpServerRequest httpServerRequest) {
        return Mono.error(new IllegalAccessException("Access forbidden by me"));
    }

    @RequestMapping(method = GET, path = "/yes/{name}", rank = 1)
    public Flux<String> yes(HttpServerRequest httpServerRequest) {
        return Flux.just("Hey " + httpServerRequest.param("name"), "Hey baby !");
    }

    @RequestMapping(method = POST, path = "yes")
    public Mono<Pojo> postPojo(HttpServerRequest httpServerRequest) {
        return Mono.from(
                codecManager.decodeAs(httpServerRequest, new TypeToken<Pojo>() {
                }))
                .map(pojo -> new Pojo(pojo.id, pojo.name + " from server"));
    }

}
