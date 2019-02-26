package com.liveaction.reactiff.codec.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.codec.jackson.model.Pojo;
import io.netty.buffer.ByteBuf;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;

public class SmileBinaryCodecTest {

    private SmileBinaryCodec tested;

    @Before
    public void setUp() {
        tested = new SmileBinaryCodec(new ObjectMapper());
    }

    @Test
    public void shouldDeserializeFluxAsStream() {
        StepVerifier.withVirtualTime(() -> {
            Flux<Pojo> toEncode = Flux.range(0, 3)
                    .delayElements(Duration.ofMillis(1000))
                    .map(i -> new Pojo("test", "value_" + i));
            Publisher<ByteBuf> byteBufFlux = Flux.from(tested.encode("application/octet-stream", toEncode, TypeToken.of(Pojo.class)));
            return tested.decodeFlux("application/octet-stream", byteBufFlux, new TypeToken<Pojo>() {
            });
        })
                .expectSubscription()
                .expectNoEvent(Duration.ofMillis(1000))
                .expectNext(new Pojo("test", "value_0"))
                .expectNoEvent(Duration.ofMillis(1000))
                .expectNext(new Pojo("test", "value_1"))
                .expectNoEvent(Duration.ofMillis(1000))
                .expectNext(new Pojo("test", "value_2"))
                .expectComplete()
                .verify(Duration.ofMillis(200));
    }

}