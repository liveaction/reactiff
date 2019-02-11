package com.liveaction.reactiff.codec.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Objects;

public class JsonCodecTest {

    private JsonCodec tested;

    @Before
    public void setUp() throws Exception {
        tested = new JsonCodec();
        tested.setObjectMapper(new ObjectMapper());
    }

    @Test
    public void shouldDeserializeArray() {
        StepVerifier.withVirtualTime(() -> {
            Flux<Pojo> toEncode = Flux.range(0, 3)
                    .delayElements(Duration.ofMillis(1000))
                    .map(i -> new Pojo("test", "value_" + i));
            Publisher<ByteBuf> byteBufFlux = Flux.from(tested.encode("test", toEncode))
                    .doOnNext(byteBuf -> System.out.println("new bytebuf"));
            return tested.decode("test", byteBufFlux, new TypeToken<Pojo>() {
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

        public Pojo(@JsonProperty("type") String type, @JsonProperty("value") String value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pojo pojo = (Pojo) o;
            return Objects.equals(type, pojo.type) &&
                    Objects.equals(value, pojo.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, value);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Pojo{");
            sb.append("type='").append(type).append('\'');
            sb.append(", value='").append(value).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
}