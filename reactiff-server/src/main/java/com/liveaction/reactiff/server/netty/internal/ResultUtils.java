package com.liveaction.reactiff.server.netty.internal;

import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.server.netty.Result;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public final class ResultUtils {

    @SuppressWarnings("unchecked")
    public static Mono<Result<?>> toResult(TypeToken<?> returnType, Object result) {
        Class<?> rawType = returnType.getRawType();

        if (Mono.class.isAssignableFrom(rawType)) {
            TypeToken<?> paramType = returnType.resolveType(Mono.class.getTypeParameters()[0]);
            if (Result.class.isAssignableFrom(paramType.getClass())) {
                return (Mono<Result<?>>) result;
            }
            Mono<?> publisher = (Mono) result;
            return publisher.flatMap(mono -> Mono.just(Result.ok(Mono.just(mono))));

        } else if (Publisher.class.isAssignableFrom(rawType)) {
            TypeToken<?> paramType = returnType.resolveType(Publisher.class.getTypeParameters()[0]);
            if (Result.class.isAssignableFrom(paramType.getClass())) {
                return Mono.from((Publisher<Result<?>>) result);
            }
            Publisher<?> publisher = (Publisher) result;
            return Mono.just(Result.ok(publisher));

        } else if (Result.class.isAssignableFrom(rawType)) {
            Result<?> httpResult = (Result<?>) result;
            return Mono.just(httpResult);

        } else {
            return Mono.just(Result.ok(Mono.just(result)));
        }
    }

}
