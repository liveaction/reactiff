package com.liveaction.reactiff.codec.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.common.base.Throwables;
import com.google.common.collect.*;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.codec.jackson.model.Pojo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuples;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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


    public <T> Stream<byte[]> encode(Stream<T> vals) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        SmileFactory smileFactory = new SmileFactory();
        JsonGenerator generator = smileFactory.createGenerator(byteArrayOutputStream);

        return vals.map(val -> {
            try {
                byteArrayOutputStream.reset();
                generator.writeObject(val);
                generator.flush();
                return byteArrayOutputStream.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void fluxOfString() {
        ObjectMapper objectCodec = new ObjectMapper();
        objectCodec.registerModule(new GuavaModule());
        objectCodec.registerModule(new ParameterNamesModule());
        SmileBinaryCodec codec = new SmileBinaryCodec(objectCodec);

        Flux<String> just = Flux.just("class=cluster,client=31,cluster=client", "class=cluster,client=37,cluster=client");

        Publisher<ByteBuf> encode = codec.encode(SmileBinaryCodec.APPLICATION_BINARY, just, new TypeToken<String>() {
        });
        List<String> block = codec.decodeFlux(SmileBinaryCodec.APPLICATION_BINARY, encode, new TypeToken<String>() {
        }).collectList().block();
        System.out.println(block);
    }

    @Test
    public void listOfString() {
        ObjectMapper objectCodec = new ObjectMapper();
        objectCodec.registerModule(new GuavaModule());
        objectCodec.registerModule(new ParameterNamesModule());
        SmileBinaryCodec codec = new SmileBinaryCodec(objectCodec);

        List<String> just = Lists.newArrayList("class=cluster,client=31,cluster=client", "class=cluster,client=37,cluster=client");

        Publisher<ByteBuf> encode = codec.encode(SmileBinaryCodec.APPLICATION_BINARY, Mono.just(just), new TypeToken<List<String>>() {
        });
        List<String> block = codec.decodeFlux(SmileBinaryCodec.APPLICATION_BINARY, encode, new TypeToken<List<String>>() {
        }).last().block();
        System.out.println(block);
    }

    @Test
    public void fluxOfPojo() {
        ObjectMapper objectCodec = new ObjectMapper();
        objectCodec.registerModule(new GuavaModule());
        objectCodec.registerModule(new ParameterNamesModule());
        SmileBinaryCodec codec = new SmileBinaryCodec(objectCodec);

        Flux<Pojo> just = Flux.just(new Pojo("name1", "value1"), new Pojo("name2", "value2"));
        Publisher<ByteBuf> encode = codec.encode(SmileBinaryCodec.APPLICATION_BINARY, just, new TypeToken<Pojo>() {
        });
        List<Pojo> block = codec.decodeFlux(SmileBinaryCodec.APPLICATION_BINARY, encode, new TypeToken<Pojo>() {
        }).collectList().block();
        System.out.println(block);
    }

    @Test
    public void fluxOfString_json() {
        ObjectMapper objectCodec = new ObjectMapper();
        objectCodec.registerModule(new GuavaModule());
        objectCodec.registerModule(new ParameterNamesModule());
        JsonCodec codec = new JsonCodec(objectCodec);

        Flux<String> just = Flux.just("class=cluster,client=31,cluster=client", "class=cluster,client=37,cluster=client", "class=cluster,client=37,cluster=client");

        Publisher<ByteBuf> encode = codec.encode(JsonCodec.APPLICATION_STREAM_JSON, just, new TypeToken<String>() {
        });
        List<String> block = codec.decodeFlux(JsonCodec.APPLICATION_STREAM_JSON, encode, new TypeToken<String>() {
        }).collectList().block();
        System.out.println(block);
    }

    @Test
    public void fluxOfPojo_json() {
        ObjectMapper objectCodec = new ObjectMapper();
        objectCodec.registerModule(new GuavaModule());
        objectCodec.registerModule(new ParameterNamesModule());
        JsonCodec codec = new JsonCodec(objectCodec);

        Flux<Pojo> just = Flux.just(new Pojo("name1", "value1"), new Pojo("name2", "value2"));
        Publisher<ByteBuf> encode = codec.encode(JsonCodec.APPLICATION_STREAM_JSON, just, new TypeToken<Pojo>() {
        });
        List<Pojo> block = codec.decodeFlux(JsonCodec.APPLICATION_STREAM_JSON, encode, new TypeToken<Pojo>() {
        }).collectList().block();
        System.out.println(block);
    }

    private <T> Flux<ByteBuf> encodeValue(Flux<T> values, ObjectMapper smileMapper) {
        return Flux.using(() -> {
            ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();
            ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(byteBuf);
            JsonGenerator generator;
            try {
                generator = smileMapper.getFactory().createGenerator((OutputStream) byteBufOutputStream);
                return Tuples.of(byteBuf, byteBufOutputStream, generator);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }, tuple -> {
            ByteBuf byteBuf = tuple.getT1();
            JsonGenerator generator = tuple.getT3();
            return values.concatMap(val -> {
                try {
                    byteBuf.clear();
                    smileMapper.writeValue(generator, val);
                    generator.flush();
                    return Mono.just(byteBuf.copy());
                } catch (IOException e) {
                    return Mono.error(e);
                }
            });
        }, tuple -> {
            try {
                tuple.getT3().close();
                tuple.getT2().close();
                tuple.getT1().release();
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        });
    }
}