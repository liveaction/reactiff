package com.liveaction.reactiff.server.utils;

import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.server.Result;
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
            return publisher.map(mono -> toTypedResult(Mono.just(mono), paramType));

        } else if (PUBLISHER_TYPE_TOKEN.isAssignableFrom(returnType)) {
            TypeToken<?> paramType = returnType.resolveType(Publisher.class.getTypeParameters()[0]);
            if (RESULT_TYPE_TOKEN.isAssignableFrom(paramType)) {
                return Mono.error(new IllegalStateException("A ReactiveHandler cannot return Flux<Result>, use Mono<Result> instead"));
            }
            return Mono.just(toTypedResult(result, paramType));

        } else if (RESULT_TYPE_TOKEN.isAssignableFrom(returnType)) {
            Result httpResult = (Result) result;
            return Mono.just(httpResult);

        } else {
            return Mono.just(toTypedResult(Mono.just(result), returnType));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Result<T> toTypedResult(Object result, TypeToken<T> typeToken) {
        return Result.ok((Publisher<T>) result, typeToken);
    }

}
