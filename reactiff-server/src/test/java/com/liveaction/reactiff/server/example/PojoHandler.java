package com.liveaction.reactiff.server.example;

import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.api.server.annotation.RequestMapping;
import com.liveaction.reactiff.server.mock.Pojo;
import reactor.core.publisher.Flux;

import java.time.Duration;

import static com.liveaction.reactiff.api.server.HttpMethod.GET;

public final class PojoHandler implements ReactiveHandler {

    @RequestMapping(method = GET, path = "/pojo")
    public Flux<Pojo> list() {
        return Flux.range(0, 10)
                .delayElements(Duration.ofMillis(50))
                .map(index -> new Pojo(String.valueOf(index), "Hello you"));
    }

}