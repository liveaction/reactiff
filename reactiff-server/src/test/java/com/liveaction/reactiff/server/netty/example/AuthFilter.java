package com.liveaction.reactiff.server.netty.example;

import com.liveaction.reactiff.server.netty.AnnotationReactiveFilter;
import com.liveaction.reactiff.server.netty.FilterChain;
import com.liveaction.reactiff.server.netty.Request;
import com.liveaction.reactiff.server.netty.Result;
import reactor.core.publisher.Mono;

public class AuthFilter implements AnnotationReactiveFilter<RequiresAuth> {

    @Override
    public Class<RequiresAuth> annotation() {
        return RequiresAuth.class;
    }

    @Override
    public Mono<Result> annotatedFilter(Request request, FilterChain chain, RequiresAuth requiresAuth) {
        if (requiresAuth.authorized()) {
            return chain.chain(request);
        } else {
            return Mono.error(new IllegalAccessException("RequiresAuth is not authorized"));
        }
    }

}
