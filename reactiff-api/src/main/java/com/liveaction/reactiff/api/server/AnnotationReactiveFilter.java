package com.liveaction.reactiff.api.server;

import reactor.core.publisher.Mono;

import java.lang.annotation.Annotation;

public interface AnnotationReactiveFilter<T extends Annotation> extends ReactiveFilter {

    Class<T> annotation();

    @Override
    default Mono<Result> filter(Request request, FilterChain chain) {
        return request.matchingRoute()
                .map(route -> {
                    T annotation = route.handlerMethod().getDeclaredAnnotation(annotation());
                    if (annotation != null) {
                        return annotatedFilter(request, chain, annotation);
                    } else {
                        return chain.chain(request);
                    }
                })
                .orElseGet(() -> chain.chain(request));
    }

    Mono<Result> annotatedFilter(Request request, FilterChain chain, T annotation);

}
