package com.liveaction.reactiff.codec.jackson;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.liveaction.reactiff.codec.jackson.model.PojoValues;
import com.liveaction.reactiff.codec.jackson.model.PojoValuesList;
import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BinaryCodecTest {

    private ObjectMapper jsonMapper = new ObjectMapper().registerModules(new GuavaModule(), new ParameterNamesModule());
    private ObjectMapper smileMapper = new ObjectMapper(new SmileFactory()).registerModules(new GuavaModule(), new ParameterNamesModule());

    @Test
    public void shouldSerializeString_Smile() throws IOException {
        Object test = "test_string";
        byte[] smileData = smileMapper.writeValueAsBytes(test);
        String otherValue = smileMapper.readValue(smileData, String.class);
        Assertions.assertThat(otherValue).isEqualTo(test);
    }

    @Test
    public void shouldSerializeString_PojoList() throws IOException {
        PojoValuesList data = new PojoValuesList(ImmutableList.of(new PojoValues("type", "val")));
        byte[] bytes = jsonMapper.writeValueAsBytes(data);
//        System.out.println(new String(bytes));
        PojoValuesList otherValue = jsonMapper.readValue(bytes, PojoValuesList.class);
        Assertions.assertThat(otherValue).isEqualTo(data);
    }

    @Test
    public void shouldSerializePojo_Smile() throws IOException {
        Object test = new PojoValues("typo", "value");
        byte[] smileData = smileMapper.writeValueAsBytes(test);
        PojoValues otherValue = smileMapper.readValue(smileData, PojoValues.class);
        Assertions.assertThat(otherValue).isEqualTo(test);
    }

    @Test
    public void shouldSerializePojoList_Smile() throws IOException {
        Object test = new PojoValuesList(ImmutableList.of(new PojoValues("typo", "value"), new PojoValues("typo", "value2")));
        byte[] data = smileMapper.writeValueAsBytes(test);
//        System.out.println(new String(data));
        PojoValuesList otherValue = smileMapper.readValue(data, PojoValuesList.class);
        Assertions.assertThat(otherValue).isEqualTo(test);
    }

    @Test
    public void shouldSerializePojo_Smile_List() throws IOException {
        Object test = ImmutableList.of(new PojoValues("typo", "value"), new PojoValues("typo", "value2"));
        byte[] smileData = smileMapper.writeValueAsBytes(test);
//        System.out.println(new String(smileData));
        ImmutableList<PojoValues> otherValue = smileMapper.readValue(smileData, new TypeReference<ImmutableList<PojoValues>>() {
        });
        Assertions.assertThat(otherValue).isEqualTo(test);
    }

    @Test
    @Ignore
    public void testBinaryPerf() throws IOException {
        perf(jsonMapper.writer(), jsonMapper.reader(), "json");
        perf(smileMapper.writer(), smileMapper.reader(), "smile");
    }

    public void perf(ObjectWriter writer, ObjectReader mapper, String name) throws IOException {
        int count = 10_000;
        List<PojoValues> data = IntStream.range(0, count)
                .mapToObj(i -> new PojoValues("type_" + i, "value_" + i))
                .collect(Collectors.toList());
        PojoValuesList input = new PojoValuesList(data);

        writer.writeValueAsBytes(input);  // warmup ?
        Stopwatch timer = Stopwatch.createStarted();
        byte[] bytes = writer.writeValueAsBytes(input);
        for (int i = 0; i < 100; i++) {
            writer.writeValueAsBytes(input);
        }
        long serializationDuration = timer.elapsed(TimeUnit.MILLISECONDS);
        mapper.forType(new TypeReference<PojoValuesList>() {
        }).readValue(bytes); // warmup ?
        timer.reset().start();
        PojoValuesList otherValue = mapper.forType(new TypeReference<PojoValuesList>() {
        }).readValue(bytes); // warmup ?
        for (int i = 0; i < 100; i++) {
            mapper.forType(new TypeReference<PojoValuesList>() {
            }).readValue(bytes); // warmup ?

        }
        long deserializationDuration = timer.elapsed(TimeUnit.MILLISECONDS);
        Assertions.assertThat(otherValue).isEqualTo(input);
        System.out.println(name);
        System.out.println("serialization " + serializationDuration + " ms");
        System.out.println("deserialization " + deserializationDuration + " ms");
        System.out.println("data.lenght " + (bytes.length / 1000) + "k");
        System.out.println();
    }

}