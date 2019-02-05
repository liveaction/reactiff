package com.liveaction.reactiff.server.netty.internal;

import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.server.netty.Result;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public final class ResultUtils {

    private static final TypeToken<Mono> MONO_TYPE_TOKEN = TypeToken.of(Mono.class);
    private static final TypeToken<Result> RESULT_TYPE_TOKEN = TypeToken.of(Result.class);
    private static final TypeToken<Publisher> PUBLISHER_TYPE_TOKEN = TypeToken.of(Publisher.class);

    @SuppressWarnings("unchecked")
    public static Mono<Result> toResult(TypeToken<?> returnType, Object result) {
        if (MONO_TYPE_TOKEN.isAssignableFrom(returnType)) {
            TypeToken<?> paramType = returnType.resolveType(Mono.class.getTypeParameters()[0]);
            if (RESULT_TYPE_TOKEN.isAssignableFrom(paramType)) {
                return (Mono<Result>) result;
            }
            Mono<?> publisher = (Mono) result;
            return publisher.flatMap(mono -> Mono.just(Result.ok(Mono.just(mono))));

        } else if (PUBLISHER_TYPE_TOKEN.isAssignableFrom(returnType)) {
            TypeToken<?> paramType = returnType.resolveType(Publisher.class.getTypeParameters()[0]);
            if (RESULT_TYPE_TOKEN.isAssignableFrom(paramType)) {
                return Mono.from((Publisher<Result>) result);
            }
            Publisher<?> publisher = (Publisher) result;
            return Mono.just(Result.ok(publisher));

        } else if (RESULT_TYPE_TOKEN.isAssignableFrom(returnType)) {
            Result httpResult = (Result) result;
            return Mono.just(httpResult);

        } else {
            return Mono.just(Result.ok(Mono.just(result)));
        }
    }

}
