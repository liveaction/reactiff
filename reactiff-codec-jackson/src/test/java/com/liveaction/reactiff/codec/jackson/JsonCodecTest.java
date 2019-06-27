package com.liveaction.reactiff.codec.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.codec.jackson.model.*;
import io.netty.buffer.ByteBuf;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.test.StepVerifier;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import static com.google.common.base.Charsets.UTF_8;

public class JsonCodecTest {

    private JsonCodec tested;

    @Before
    public void setUp() {
        ObjectMapper objectCodec = new ObjectMapper();
        objectCodec.registerModule(new GuavaModule());
        objectCodec.registerModule(new ParameterNamesModule());
        tested = new JsonCodec(objectCodec);
    }

    @Test
    public void shouldUseNewModule() {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonCodec jsonCodec = new JsonCodec(objectMapper);
        StepVerifier.create(Flux.from(jsonCodec.encode("application/json", Mono.just(new ModuledPojo("myType", "myVal")), TypeToken.of(ModuledPojo.class))))
                .expectError(InvalidDefinitionException.class)
                .verify();

        objectMapper.registerModule(new PojoJacksonModule());
        StepVerifier.create(Flux.from(jsonCodec.encode("application/json", Mono.just(new ModuledPojo("myType", "myVal")), TypeToken.of(ModuledPojo.class))))
                .expectError(InvalidDefinitionException.class)
                .verify();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new PojoJacksonModule());
        jsonCodec.reloadMapper(objectMapper);
        StepVerifier.create(Flux.from(jsonCodec.encode("application/json", Mono.just(new ModuledPojo("myType", "myVal")), TypeToken.of(ModuledPojo.class)))
                .map(byteBuf -> {
                    byte[] bytes = new byte[byteBuf.readableBytes()];
                    byteBuf.getBytes(0, bytes);
                    return new String(bytes);
                }))
                .expectNext("\"myType_myVal\"")
                .verifyComplete();
    }

    @Test
    public void shouldDeserializeFluxAsStream() {
        StepVerifier.withVirtualTime(() -> {
            Flux<Pojo> toEncode = Flux.range(0, 3)
                    .delayElements(Duration.ofMillis(1000))
                    .map(i -> new Pojo("test", "value_" + i));
            Publisher<ByteBuf> byteBufFlux = Flux.from(tested.encode("application/stream+json", toEncode, TypeToken.of(Pojo.class)));
            return tested.decodeFlux("application/stream+json", byteBufFlux, new TypeToken<Pojo>() {
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

    @Test
    public void shouldDeserializeArray() {
        StepVerifier.withVirtualTime(() -> {
            Flux<Pojo> toEncode = Flux.range(0, 3)
                    .delayElements(Duration.ofMillis(1000))
                    .map(i -> new Pojo("test", "value_" + i));
            Publisher<ByteBuf> byteBufFlux = Flux.from(tested.encode("application/json", toEncode, TypeToken.of(Pojo.class)));
            return tested.decodeFlux("application/json", byteBufFlux, new TypeToken<Pojo>() {
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

    @Test
    public void shouldSerializeComplexObject() throws IOException {
        Mono<ImmutableList<PojoMap>> toEncode = Flux.range(0, 3)
                .map(i -> new PojoMap(ImmutableMap.of(MapKey.fromString("Hey:baby"), ImmutableMap.of("You", new Pojo("gag" + i, "oug")))))
                .collectList()
                .map(ImmutableList::copyOf);
        String actual = ByteBufFlux.fromInbound(tested.encode("application/json", toEncode, new TypeToken<ImmutableList<PojoMap>>() {
        })).aggregate().asString(UTF_8).block();

        Assertions.assertThat(actual).isEqualTo(Files.toString(new File(getClass().getResource("/expected_pojomap.json").getFile()), UTF_8));
    }

}