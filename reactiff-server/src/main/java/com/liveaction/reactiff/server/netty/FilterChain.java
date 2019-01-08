package com.liveaction.reactiff.server.netty;

import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

@FunctionalInterface
public interface FilterChain {

    Mono<Void> chain(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse);

}
