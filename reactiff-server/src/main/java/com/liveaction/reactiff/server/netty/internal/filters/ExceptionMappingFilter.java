package com.liveaction.reactiff.server.netty.internal.filters;

import com.liveaction.reactiff.server.netty.FilterChain;
import com.liveaction.reactiff.server.netty.ReactiveFilter;
import com.liveaction.reactiff.server.netty.Request;
import com.liveaction.reactiff.server.netty.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public class ExceptionMappingFilter implements ReactiveFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionMappingFilter.class);

    private final Function<Throwable, Integer> mapping;

    public ExceptionMappingFilter(Function<Throwable, Integer> mapping) {
        this.mapping = mapping;
    }

    @Override
    public Mono<Result> filter(Request request, FilterChain chain) {
        return chain.chain(request)
                .onErrorResume(throwable -> {
                    Integer mappedStatus = mapping.apply(throwable);
                    int status;
                    if (mappedStatus != null) {
                        status = mappedStatus;
                    } else {
                        status = 500;
                        LOGGER.error("Unexpected error", throwable);
                    }
                    return Mono.just(Result.withStatus(status, throwable.getMessage()));
                });
    }

    @Override
    public int filterRank() {
        return Integer.MIN_VALUE;
    }

}
