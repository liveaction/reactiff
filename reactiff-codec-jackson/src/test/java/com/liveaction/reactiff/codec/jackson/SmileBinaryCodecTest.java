package com.liveaction.reactiff.codec.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.common.collect.*;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.codec.jackson.model.Pojo;
import io.netty.buffer.ByteBuf;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Test
    public void shouldDeserializeFluxOfRanges() {
        ObjectMapper objectCodec = new ObjectMapper();
        objectCodec.registerModule(new GuavaModule());
        objectCodec.registerModule(new ParameterNamesModule());
        SmileBinaryCodec smileBinaryCodec = new SmileBinaryCodec(objectCodec);
        JsonCodec jsonCodec = new JsonCodec(objectCodec);

        ImmutableMap<String, ImmutableRangeSet<String>> input = ImmutableMap.<String, ImmutableRangeSet<String>>builder()
                .put("iwan/2314459/snmp/wanLink/hour", ImmutableRangeSet.of())
                .put("iwan/2314459/snmp/wanLink/day", ImmutableRangeSet.of(Range.closed("2019-05-03T00:00:00Z", "2019-05-03T00:00:00Z")))
                .put("iwan/1230362/snmp/wanLink/day", ImmutableRangeSet.<String>builder()
                        .add(Range.closed("2019-04-25T00:00:00Z", "2019-04-25T00:00:00Z"))
                        .add(Range.closed("2019-05-03T00:00:00Z", "2019-05-03T00:00:00Z"))
                        .build())
                .build();

        List<Map.Entry<String, ImmutableSet<Range<String>>>> inputAsList = input.entrySet().stream()
                .map(entry -> Maps.immutableEntry(entry.getKey(), entry.getValue().asRanges()))
                .collect(Collectors.toList());

        TypeToken<Map.Entry<String, ImmutableSet<Range<String>>>> typeToken = new TypeToken<Map.Entry<String, ImmutableSet<Range<String>>>>() {
        };
        List<Map.Entry<String, ImmutableSet<Range<String>>>> actual;
        Publisher<ByteBuf> encode;

        encode = Flux.from(smileBinaryCodec.encode(SmileBinaryCodec.APPLICATION_BINARY, Flux.fromIterable(inputAsList), typeToken));
        actual = smileBinaryCodec.decodeFlux(JsonCodec.APPLICATION_STREAM_JSON, encode, typeToken).collectList().block();
        Assertions.assertThat(actual).isEqualTo(inputAsList);

        encode = Flux.from(jsonCodec.encode(JsonCodec.APPLICATION_STREAM_JSON, Flux.fromIterable(inputAsList), typeToken));
        actual = jsonCodec.decodeFlux(JsonCodec.APPLICATION_STREAM_JSON, encode, typeToken).collectList().block();
        Assertions.assertThat(actual).isEqualTo(inputAsList);

        encode = Flux.from(jsonCodec.encode(JsonCodec.APPLICATION_JSON, Flux.fromIterable(inputAsList), typeToken));
        actual = jsonCodec.decodeFlux(JsonCodec.APPLICATION_JSON, encode, typeToken).collectList().block();
        Assertions.assertThat(actual).isEqualTo(inputAsList);
    }
}