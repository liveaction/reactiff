package com.liveaction.reactiff.server.netty.example;

import com.liveaction.reactiff.server.netty.FilterChain;
import com.liveaction.reactiff.server.netty.ReactiveFilter;
import com.liveaction.reactiff.server.netty.Result;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

public class AuthFilter implements ReactiveFilter {

    @Override
    public Mono<Result<?>> filter(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse, FilterChain chain) {
        if (httpServerRequest.uri().startsWith("/yes")) {
            return chain.chain(httpServerRequest, httpServerResponse);
        } else {
            return Mono.error(new IllegalAccessException("not yes routes are forbidden"));
        }
    }

    @Override
    public int rank() {
        return 0;
    }

}
