package com.liveaction.reactiff.server.filters;

import com.liveaction.reactiff.api.server.FilterChain;
import com.liveaction.reactiff.api.server.ReactiveFilter;
import com.liveaction.reactiff.api.server.Request;
import com.liveaction.reactiff.api.server.Result;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public class ExceptionMappingFilter implements ReactiveFilter {

    private final Function<Throwable, Integer> mapping;

    public ExceptionMappingFilter(Function<Throwable, Integer> mapping) {
        this.mapping = mapping;
    }

    @Override
    public Mono<Result> filter(Request request, FilterChain chain) {
        return chain.chain(request)
                .onErrorResume(throwable -> {
                    Integer mappedStatus = mapping.apply(throwable);
                    if (mappedStatus != null) {
                        return Mono.just(
                                Result.<String>builder()
                                        .status(mappedStatus, throwable.getMessage())
                                        .data(Mono.just(throwable.getMessage()), String.class)
                                        .build()
                        );
                    } else {
                        return Mono.error(throwable);
                    }
                });
    }

    @Override
    public int filterRank() {
        return Integer.MIN_VALUE;
    }

}
