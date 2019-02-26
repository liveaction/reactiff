package com.liveaction.reactiff.codec.jackson;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
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
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.liveaction.reactiff.codec.jackson.model.PojoValues;
import com.liveaction.reactiff.codec.jackson.model.PojoValuesList;
import org.apache.avro.Schema;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nustaq.serialization.FSTConfiguration;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BinaryCodecTest {

    private Kryo kryo;
    private ObjectMapper jsonMapper = new ObjectMapper().registerModules(new GuavaModule(), new ParameterNamesModule());
    private ObjectMapper smileMapper = new ObjectMapper(new SmileFactory()).registerModules(new GuavaModule(), new ParameterNamesModule());
    private ObjectMapper avroMapper = new ObjectMapper(new AvroFactory()).registerModules(new GuavaModule(), new ParameterNamesModule());
    private ObjectMapper protobufMapper = new ObjectMapper(new ProtobufFactory()).registerModules(new GuavaModule(), new ParameterNamesModule());
    private ObjectMapper cborMapper = new ObjectMapper(new CBORFactory()).registerModules(new GuavaModule(), new ParameterNamesModule());
    private ObjectMapper ionMapper = new ObjectMapper(new IonFactory()).registerModules(new GuavaModule(), new ParameterNamesModule());

    @Before
    public void setUp() {
        kryo = new Kryo();
        SerializerFactory.FieldSerializerFactory serializer = new SerializerFactory.FieldSerializerFactory();
        kryo.setRegistrationRequired(false);
        kryo.setDefaultSerializer(serializer);
    }

    @Test
    @Ignore
    public void shouldSerializePojoValues() throws IOException {
        Object object = new PojoValues("typo", "value");

        Path path = Paths.get("/tmp/test-binary");
        FileOutputStream fileOutputStream = new FileOutputStream(path.toFile());
        FileInputStream fileInputStream = new FileInputStream(path.toFile());
        try (Output output = new Output(fileOutputStream)) {
            kryo.writeObject(output, object);
        }
        try (Input intput = new Input(fileInputStream)) {
            PojoValues readed = kryo.readObject(intput, PojoValues.class);
//            System.out.println("readed : " + readed);
        }
    }

    @Test
    public void shouldSerializeString_FST() {
        FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
        Object test = "test_string";
        byte[] barray = conf.asByteArray(test);
        String object = (String) conf.asObject(barray);
        Assertions.assertThat(object).isEqualTo(test);
    }

    @Test
    @Ignore
    public void shouldSerializePojo_FST() {
        FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
        Object test = new PojoValues("typo", "value");
        byte[] barray = conf.asByteArray(test);
        String object = (String) conf.asObject(barray);
        Assertions.assertThat(object).isEqualTo(test);
    }

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
    public void shouldSerializePojo_Ion() throws IOException {
        Object test = new PojoValues("typo", "value");
        byte[] data = ionMapper.writeValueAsBytes(test);
//        System.out.println(new String(data));
        PojoValues otherValue = ionMapper.readValue(data, PojoValues.class);
        Assertions.assertThat(otherValue).isEqualTo(test);
    }

    @Test
    public void shouldSerializePojoList_Ion() throws IOException {
        Object test = new PojoValuesList(ImmutableList.of(new PojoValues("typo", "value"), new PojoValues("typo", "value2")));
        byte[] data = ionMapper.writeValueAsBytes(test);
//        System.out.println(new String(data));
        PojoValuesList otherValue = ionMapper.readValue(data, PojoValuesList.class);
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
        String SCHEMA_JSON = "{\n" +
                "  \"name\": \"PojoValuesList\",\n" +
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
                "            },\n" +
                "            {\n" +
                "              \"name\": \"value2\",\n" +
                "              \"type\": \"string\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"name\": \"value3\",\n" +
                "              \"type\": \"string\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"name\": \"value4\",\n" +
                "              \"type\": \"string\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"name\": \"value5\",\n" +
                "              \"type\": \"string\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"name\": \"value6\",\n" +
                "              \"type\": \"string\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"name\": \"value7\",\n" +
                "              \"type\": \"string\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"name\": \"value8\",\n" +
                "              \"type\": \"string\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"name\": \"value9\",\n" +
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

        String protobuf_str = "message PojoValuesList {\n" +
                "  repeated Pojo pojos = 1;\n" +
                "}\n" +
                "\n" +
                "message Pojo {\n" +
                "  required string type = 1;\n" +
                "  required string value = 2;\n" +
                "  required string value2 = 3;\n" +
                "  required string value3 = 4;\n" +
                "  required string value4 = 5;\n" +
                "  required string value5 = 6;\n" +
                "  required string value6 = 7;\n" +
                "  required string value7 = 8;\n" +
                "  required string value8 = 9;\n" +
                "  required string value9 = 10;\n" +
                "}";
        ProtobufSchema protobufSchema = ProtobufSchemaLoader.std.parse(protobuf_str);

        perf(jsonMapper.writer(), jsonMapper.reader(), "json");
        perf(smileMapper.writer(), smileMapper.reader(), "smile");
        perf(avroMapper.writer(avroSchema), avroMapper.reader(avroSchema), "avro");
        perf(protobufMapper.writer(protobufSchema), protobufMapper.reader(protobufSchema), "protobuf");
        perf(cborMapper.writer(), cborMapper.reader(), "cbor");
        perf(ionMapper.writer(), ionMapper.reader(), "ion");
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

    @Test
    public void shouldSerializeString_Cbor() throws IOException {
        Object test = "test_string";
        byte[] smileData = cborMapper.writeValueAsBytes(test);
        String otherValue = cborMapper.readValue(smileData, String.class);
        Assertions.assertThat(otherValue).isEqualTo(test);
    }

    @Test
    public void shouldSerializePojo_Cbor() throws IOException {
        Object test = new PojoValues("typo", "value");
        byte[] smileData = cborMapper.writeValueAsBytes(test);
        PojoValues otherValue = cborMapper.readValue(smileData, PojoValues.class);
        Assertions.assertThat(otherValue).isEqualTo(test);
    }

    @Test
    public void shouldSerializePojo_Cbor_List() throws IOException {
        Object test = ImmutableList.of(new PojoValues("typo", "value"), new PojoValues("typo", "value2"));
        byte[] smileData = cborMapper.writeValueAsBytes(test);
//        System.out.println(new String(smileData));
        ImmutableList<PojoValues> otherValue = cborMapper.readValue(smileData, new TypeReference<ImmutableList<PojoValues>>() {
        });
        Assertions.assertThat(otherValue).isEqualTo(test);
    }

}