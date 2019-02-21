package com.liveaction.reactiff.codec.binary;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.avro.AvroFactory;
import com.fasterxml.jackson.dataformat.avro.AvroSchema;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.ion.IonFactory;
import com.fasterxml.jackson.dataformat.protobuf.ProtobufFactory;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchemaLoader;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import org.apache.avro.Schema;
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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    public void shouldSerializeString_PojoList() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new ParameterNamesModule());
        PojoList data = new PojoList(ImmutableList.of(new Pojo("type", "val")));
        byte[] bytes = mapper.writeValueAsBytes(data);
        System.out.println(new String(bytes));
        PojoList otherValue = mapper.readValue(bytes, PojoList.class);
        Assertions.assertThat(otherValue).isEqualTo(data);
    }

    @Test
    public void shouldSerializePojo_Smile() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new SmileFactory());
        mapper.registerModule(new ParameterNamesModule());
        Object test = new Pojo("typo", "value");
        byte[] smileData = mapper.writeValueAsBytes(test);
        Files.write(Paths.get("/tmp/test-smile"), smileData);
        Pojo otherValue = mapper.readValue(smileData, Pojo.class);
        Assertions.assertThat(otherValue).isEqualTo(test);
    }

    @Test
    public void shouldSerializePojo_Smile_List() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new SmileFactory());
        mapper.registerModules(new GuavaModule(), new ParameterNamesModule());
        Object test = ImmutableList.of(new Pojo("typo", "value"),new Pojo("typo", "value2"));
        byte[] smileData = mapper.writeValueAsBytes(test);
        System.out.println(new String(smileData));
        Files.write(Paths.get("/tmp/test-smile"), smileData);
        ImmutableList<Pojo> otherValue = mapper.readValue(smileData, new TypeReference<ImmutableList<Pojo>>(){});
        Assertions.assertThat(otherValue).isEqualTo(test);
    }

    @Test
    public void testBinaryPerf() throws IOException {
        ObjectMapper jsonMapper = new ObjectMapper();
        jsonMapper.registerModules(new GuavaModule(), new ParameterNamesModule(), new Jdk8Module());

        ObjectMapper smileMapper = new ObjectMapper(new SmileFactory());
        smileMapper.registerModules(new GuavaModule(), new ParameterNamesModule());

        ObjectMapper avroMapper = new ObjectMapper(new AvroFactory());
        avroMapper.registerModules(new GuavaModule(), new ParameterNamesModule());
        String SCHEMA_JSON = "{\n" +
                "  \"name\": \"PojoList\",\n" +
                "  \"type\": \"record\",\n" +
                "  \"fields\": [\n" +
                "    {\n" +
                "      \"name\": \"pojos\",\n" +
                "      \"type\": {\n" +
                "        \"type\": \"array\",\n" +
                "        \"items\": {\n" +
                "          \"name\": \"Pojo\",\n" +
                "          \"type\": \"record\",\n" +
                "          \"fields\": [\n" +
                "            {\n" +
                "              \"name\": \"type\",\n" +
                "              \"type\": \"string\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"name\": \"value\",\n" +
                "              \"type\": \"string\"\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        Schema raw = new Schema.Parser().setValidate(true).parse(SCHEMA_JSON);
        AvroSchema avroSchema = new AvroSchema(raw);

        String protobuf_str = "message PojoList {\n" +
                "  repeated Pojo pojos = 1;\n" +
                "}\n" +
                "\n" +
                "message Pojo {\n" +
                "  required string type = 1;\n" +
                "  required string value = 2;\n" +
                "}";
        ProtobufSchema protobufSchema = ProtobufSchemaLoader.std.parse(protobuf_str);
        ObjectMapper protobufMapper = new ObjectMapper(new ProtobufFactory());

        protobufMapper.registerModules(new GuavaModule(), new ParameterNamesModule());

        ObjectMapper cborMapper = new ObjectMapper(new CBORFactory());
        cborMapper.registerModules(new GuavaModule(), new ParameterNamesModule());

        ObjectMapper ionMapper = new ObjectMapper(new IonFactory());
        ionMapper.registerModules(new GuavaModule(), new ParameterNamesModule());


        perf(jsonMapper.writer(), jsonMapper.reader(), "json");
        perf(smileMapper.writer(),smileMapper.reader(), "smile");
        perf(avroMapper.writer(avroSchema), avroMapper.reader(avroSchema), "avro");
        perf(protobufMapper.writer(protobufSchema), protobufMapper.reader(protobufSchema), "protobuf");
        perf(cborMapper.writer(), cborMapper.reader(), "cbor");
        perf(ionMapper.writer(), ionMapper.reader(), "ion");
    }

    public void perf(ObjectWriter writer, ObjectReader mapper, String name) throws IOException {
        int count = 100_000;
        List<Pojo> data = IntStream.range(0, count)
                .mapToObj(i -> new Pojo("type_" + i, "value_" + i))
                .collect(Collectors.toList());
        PojoList input = new PojoList(data);

        writer.writeValueAsBytes(input);  // warmup ?
        Stopwatch timer = Stopwatch.createStarted();
        byte[] bytes = writer.writeValueAsBytes(input);
        long serializationDuration = timer.elapsed(TimeUnit.MILLISECONDS);
        mapper.forType(new TypeReference<PojoList>() {
        }).readValue(bytes); // warmup ?
        timer.reset().start();
        PojoList otherValue = mapper.forType(new TypeReference<PojoList>() {
        }).readValue(bytes); // warmup ?
        long deserializationDuration = timer.elapsed(TimeUnit.MILLISECONDS);
        Assertions.assertThat(otherValue).isEqualTo(input);
        System.out.println(name);
        System.out.println("serialization "+serializationDuration+" ms");
        System.out.println("deserialization "+deserializationDuration+" ms");
        System.out.println("data.lenght "+(bytes.length/1000) + "k");
        System.out.println();
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
        mapper.registerModules(new GuavaModule(), new ParameterNamesModule());
        Object test = new Pojo("typo", "value");
        byte[] smileData = mapper.writeValueAsBytes(test);
        Files.write(Paths.get("/tmp/test-smile"), smileData);
        Pojo otherValue = mapper.readValue(smileData, Pojo.class);
        Assertions.assertThat(otherValue).isEqualTo(test);
    }

    @Test
    public void shouldSerializePojo_Cbor_List() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new CBORFactory());
        mapper.registerModules(new GuavaModule(), new ParameterNamesModule());
        Object test = ImmutableList.of(new Pojo("typo", "value"),new Pojo("typo", "value2"));
        byte[] smileData = mapper.writeValueAsBytes(test);
        System.out.println(new String(smileData));
        Files.write(Paths.get("/tmp/test-cbor"), smileData);
        ImmutableList<Pojo> otherValue = mapper.readValue(smileData, new TypeReference<ImmutableList<Pojo>>(){});
        Assertions.assertThat(otherValue).isEqualTo(test);
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

    public final static class PojoList {
        public final List<Pojo> pojos;

        @JsonCreator
        public PojoList(List<Pojo> pojos) {
            this.pojos = pojos;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PojoList pojoList = (PojoList) o;
            return Objects.equals(pojos, pojoList.pojos);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pojos);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("PojoList{");
            sb.append("pojos=").append(pojos);
            sb.append('}');
            return sb.toString();
        }
    }
}