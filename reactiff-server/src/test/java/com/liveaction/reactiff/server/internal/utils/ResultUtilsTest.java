package com.liveaction.reactiff.server.internal.utils;

import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.server.Result;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import reactor.core.publisher.Mono;


public class ResultUtilsTest {
    @Test
    public void shouldConvertVoid() {
        Mono<Result> resultMono = ResultUtils.toResult(new TypeToken<Void>() {
        }, null);
        Assertions.assertThat(resultMono.block().data()).isNull();

        resultMono = ResultUtils.toResult(new TypeToken<Mono<Void>>() {
        }, Mono.empty());
        Result block = resultMono.block();
        Assertions.assertThat(block.data()).isNull();
    }
}