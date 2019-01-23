package com.liveaction.reactiff.server.netty.example;

import com.liveaction.reactiff.server.netty.FilterChain;
import com.liveaction.reactiff.server.netty.ReactiveFilter;
import com.liveaction.reactiff.server.netty.Request;
import com.liveaction.reactiff.server.netty.Result;
import reactor.core.publisher.Mono;

public class AuthFilter implements ReactiveFilter {

    @Override
    public Mono<Result> filter(Request request, FilterChain chain) {
        if (request.uri().startsWith("/yes")) {
            return chain.chain(request);
        } else {
            return Mono.error(new IllegalAccessException("not yes routes are forbidden"));
        }
    }

}
