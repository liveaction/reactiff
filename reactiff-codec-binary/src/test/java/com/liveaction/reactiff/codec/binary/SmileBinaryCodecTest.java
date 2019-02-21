package com.liveaction.reactiff.codec.binary;

import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.common.base.Objects;
import com.google.common.reflect.TypeToken;
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
    public void setUp() throws Exception {
        tested = new SmileBinaryCodec();
        tested.addModule(new ParameterNamesModule());
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

    public final static class Pojo {
        public final String type;
        public final String value;

        public Pojo(String type, String value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pojo pojo = (Pojo) o;
            return Objects.equal(type, pojo.type) &&
                    Objects.equal(value, pojo.value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(type, value);
        }

        @Override
        public String toString() {
            return "Pojo{" + "type='" + type + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }
}