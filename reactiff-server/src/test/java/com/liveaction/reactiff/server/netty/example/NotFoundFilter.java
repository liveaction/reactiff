package com.liveaction.reactiff.server.netty.example;

import com.liveaction.reactiff.server.netty.FilterChain;
import com.liveaction.reactiff.server.netty.ReactiveFilter;
import com.liveaction.reactiff.server.netty.Request;
import com.liveaction.reactiff.server.netty.Result;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

public final class NotFoundFilter implements ReactiveFilter {

    @Override
    public int filterRank() {
        return Integer.MAX_VALUE;
    }

    @Override
    public Mono<Result> filter(Request request, FilterChain chain) {
        return request.matchingRoute()
                .map(r -> chain.chain(request))
                .orElse(Mono.error(new NoSuchElementException()));
    }

}