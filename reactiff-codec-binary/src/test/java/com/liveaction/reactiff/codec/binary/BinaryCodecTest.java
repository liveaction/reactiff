package com.liveaction.reactiff.codec.binary;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.avro.AvroMapper;
import com.fasterxml.jackson.dataformat.avro.AvroModule;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.nustaq.serialization.FSTConfiguration;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BinaryCodecTest {

    private Kryo kryo;

    @Before
    public void setUp() throws Exception {
        kryo = new Kryo();
        SerializerFactory.FieldSerializerFactory serializer = new SerializerFactory.FieldSerializerFactory();
        kryo.setRegistrationRequired(false);
        kryo.setDefaultSerializer(serializer);
    }

    @Test
    public void shouldSerializePojo() throws IOException {
        Object object = new Pojo("typo", "value");

//        kryo.register(Pojo.class);

        Path path = Paths.get("/tmp/test-binary");
        FileOutputStream fileOutputStream = new FileOutputStream(path.toFile());
        FileInputStream fileInputStream = new FileInputStream(path.toFile());
        try (Output output = new Output(fileOutputStream)) {
            kryo.writeObject(output, object);
        }
        try (Input intput = new Input(fileInputStream)) {
            Pojo readed = kryo.readObject(intput, Pojo.class);
            System.out.println("readed : " +readed);
        }
    }

    @Test
    public void shouldSerializeString_FST() {
        FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
        Object test = "test_string";
        byte[] barray = conf.asByteArray(test);
        String object = (String)conf.asObject(barray);
        Assertions.assertThat(object).isEqualTo(test);
    }

    @Test
    public void shouldSerializePojo_FST() {
        FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
        Object test = new Pojo("typo", "value");
        byte[] barray = conf.asByteArray(test);
        String object = (String)conf.asObject(barray);
        Assertions.assertThat(object).isEqualTo(test);
    }

    @Test
    public void shouldSerializeString_Smile() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new SmileFactory());
        Object test = "test_string";
        byte[] smileData = mapper.writeValueAsBytes(test);
        Files.write(Paths.get("/tmp/test-smile"), smileData);
        String otherValue = mapper.readValue(smileData, String.class);
        Assertions.assertThat(otherValue).isEqualTo(test);
    }

    @Test
    public void shouldSerializePojo_Smile() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new SmileFactory());
        Object test = new Pojo("typo", "value");
        byte[] smileData = mapper.writeValueAsBytes(test);
        Files.write(Paths.get("/tmp/test-smile"), smileData);
        Pojo otherValue = mapper.readValue(smileData, Pojo.class);
        Assertions.assertThat(otherValue).isEqualTo(test);
    }

    @Test
    public void shouldSerializePojo_Smile_List() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new SmileFactory());
        mapper.registerModule(new GuavaModule());
        Object test = ImmutableList.of(new Pojo("typo", "value"),new Pojo("typo", "value2"));
        byte[] smileData = mapper.writeValueAsBytes(test);
        System.out.println(new String(smileData));
        Files.write(Paths.get("/tmp/test-smile"), smileData);
        ImmutableList<Pojo> otherValue = mapper.readValue(smileData, new TypeReference<ImmutableList<Pojo>>(){});
        Assertions.assertThat(otherValue).isEqualTo(test);
    }

    @Test
    public void shouldSerializePojo_Avro_List() throws IOException {
        final ObjectMapper mapper = new AvroMapper()
                .registerModule(new GuavaModule())
                .registerModule(new AvroModule());
        Object test = ImmutableList.of(new Pojo("typo", "value"),new Pojo("typo", "value2"));
        byte[] smileData = mapper.writeValueAsBytes(test);
        Files.write(Paths.get("/tmp/test-smile"), smileData);
        ImmutableList<Pojo> otherValue = mapper.readValue(smileData, new TypeReference<ImmutableList<Pojo>>(){});
        Assertions.assertThat(otherValue).isEqualTo(test);
    }

    @Test
    public void shouldSerializeString_Cbor() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new CBORFactory());
        Object test = "test_string";
        byte[] smileData = mapper.writeValueAsBytes(test);
        Files.write(Paths.get("/tmp/test-smile"), smileData);
        String otherValue = mapper.readValue(smileData, String.class);
        Assertions.assertThat(otherValue).isEqualTo(test);
    }

    @Test
    public void shouldSerializePojo_Cbor() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new CBORFactory());
        Object test = new Pojo("typo", "value");
        byte[] smileData = mapper.writeValueAsBytes(test);
        Files.write(Paths.get("/tmp/test-smile"), smileData);
        Pojo otherValue = mapper.readValue(smileData, Pojo.class);
        Assertions.assertThat(otherValue).isEqualTo(test);
    }

    @Test
    public void shouldSerializePojo_Cbor_List() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new CBORFactory());
        mapper.registerModule(new GuavaModule());
        Object test = ImmutableList.of(new Pojo("typo", "value"),new Pojo("typo", "value2"));
        byte[] smileData = mapper.writeValueAsBytes(test);
        System.out.println(new String(smileData));
        Files.write(Paths.get("/tmp/test-cbor"), smileData);
        ImmutableList<Pojo> otherValue = mapper.readValue(smileData, new TypeReference<ImmutableList<Pojo>>(){});
        Assertions.assertThat(otherValue).isEqualTo(test);
    }

    @Test
    public void shouldSerializePojoList() throws IOException {
        Object object = Lists.newArrayList(new Pojo("typo", "value"));

        kryo.register(Pojo.class);
        kryo.register(ArrayList.class);
        kryo.register(ArrayList.class);

        Path path = Paths.get("/tmp/test-binary");
        FileOutputStream fileOutputStream = new FileOutputStream(path.toFile());
        FileInputStream fileInputStream = new FileInputStream(path.toFile());
        try (Output output = new Output(fileOutputStream)) {
            kryo.writeObject(output, object);
        }
        try (Input intput = new Input(fileInputStream)) {
            List<Pojo> readed = kryo.readObject(intput, List.class);
            System.out.println("readed : " +readed);
        }
    }

    public final static class Pojo {
        public final String type;
        public final String value;

        public Pojo() {
            this(null, null);
        }

        public Pojo(String type, String value) {
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
            return "Pojo{" + "type='" + type + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }

//    public final static class PojoList {
//
//    }
}