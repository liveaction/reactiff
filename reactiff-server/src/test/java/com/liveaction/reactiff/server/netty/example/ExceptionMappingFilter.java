package com.liveaction.reactiff.server.netty.example;

import com.liveaction.reactiff.server.netty.FilterChain;
import com.liveaction.reactiff.server.netty.NettyServerTest;
import com.liveaction.reactiff.server.netty.ReactiveFilter;
import com.liveaction.reactiff.server.netty.Result;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.util.NoSuchElementException;

public class ExceptionMappingFilter implements ReactiveFilter {

    @Override
    public Mono<Result<?>> filter(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse, FilterChain chain) {
        return chain.chain(httpServerRequest, httpServerResponse)
                .onErrorResume(throwable -> {
                    boolean caught = false;
                    int status = 500;
                    if (throwable instanceof IllegalAccessException) {
                        status = 401;
                        caught = true;
                    } else if (throwable instanceof NoSuchElementException) {
                        status = 404;
                        caught = true;
                    }
                    if (!caught) {
                        LoggerFactory.getLogger(NettyServerTest.class).error("Unexpected error", throwable);
                    }
                    return Mono.just(Result.withCode(status, throwable.getMessage()));
                });
    }

    @Override
    public int rank() {
        return -1;
    }

}
