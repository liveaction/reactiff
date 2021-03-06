package com.liveaction.reactiff.api.server;

import reactor.core.publisher.Mono;

@FunctionalInterface
public interface FilterChain {

    Mono<Result> chain(Request request);

}
