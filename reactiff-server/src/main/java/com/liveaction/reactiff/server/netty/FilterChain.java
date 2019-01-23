package com.liveaction.reactiff.server.netty;

import reactor.core.publisher.Mono;

@FunctionalInterface
public interface FilterChain {

    Mono<Result> chain(Request request);

}
