package com.liveaction.reactiff.server.netty;

import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

public interface ReactiveFilter extends Rankable<ReactiveFilter> {

    Mono<Result<?>> filter(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse, FilterChain chain);

}
