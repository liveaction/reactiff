package com.liveaction.reactiff.server.performance.implementations;

import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.server.HttpMethod;
import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.api.server.Request;
import com.liveaction.reactiff.api.server.annotation.RequestMapping;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class PerformanceController implements ReactiveHandler {

    @RequestMapping(method = HttpMethod.POST, path = "/post-timeout-server")
    public Flux<Integer> post_timeout_server(Request request) {

        return request.bodyToFlux(new TypeToken<String>() {
        })
                .timeout(Duration.ofMillis(100))
                .flatMap(s -> delayedFlux(Duration.ofMillis(200)), 1);
    }

    @RequestMapping(method = HttpMethod.POST, path = "/post-timeout")
    public Flux<Integer> post_timeout(Request request) {

        return request.bodyToFlux(new TypeToken<String>() {
        })
                .timeout(Duration.ofMillis(100))
                .flatMap(s -> delayedFlux(Duration.ofMillis(1)), 1);
    }

    @RequestMapping(method = HttpMethod.POST, path = "/post-cancel")
    public Flux<Integer> post_cancel(Request request) throws InterruptedException {

        Flux<Integer> map = request.bodyToFlux(new TypeToken<String>() {
        })
                .map(s -> 1);

        // calling next() will cancel the Flux
        map.next().subscribe();
        return map;
    }

    private Flux<Integer> delayedFlux(Duration delay) {
        return Flux.just(1)
                .delayElements(delay);
    }

}
