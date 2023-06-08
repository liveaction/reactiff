package com.liveaction.reactiff.codec.jackson;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import org.junit.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class JacksonCodecTest {
    @Test
    public void test_KO() throws IOException, ExecutionException, InterruptedException {
        Map<String, Map<String, String>> tags = new LinkedHashMap<>();
        for (int i = 0; i < 10; i++) {
            Map<String, String> value = new LinkedHashMap<>();
            for (int j = 0; j < 10; j++) {
                value.put("key_" + j, "val" + j);
            }
            tags.put("elt_" + i, value);
        }

        SmileFactory jsonFactory = new SmileFactory();
        jsonFactory.configure(JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING, false);
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectWriter objectWriter = objectMapper.writer().with(jsonFactory);
        jsonFactory.setCodec(objectMapper);
        byte[] json = objectWriter.writeValueAsBytes(tags);
        TypeReference<Map<String, Map<String, String>>> typeReference = new TypeReference<Map<String, Map<String, String>>>() {
        };

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        List<CompletableFuture<?>> futures = new ArrayList<>();

        int count = 100000;
        for (int i = 0; i < count; i++) {
            JsonParser parser = jsonFactory.createNonBlockingByteArrayParser();
            futures.add(CompletableFuture.supplyAsync(() -> {
            ByteArrayFeeder inputFeeder = null;
                try {
                    inputFeeder = (ByteArrayFeeder) parser.getNonBlockingInputFeeder();
                    inputFeeder.feedInput(json, 0, json.length);
                    TokenBuffer tokenBuffer = new TokenBuffer(parser);
                    while (true) {
                        JsonToken token = parser.nextToken();
                        if (token == JsonToken.NOT_AVAILABLE || token == null) {
                            break;
                        }

                        tokenBuffer.copyCurrentEvent(parser);
                    }
                    return tokenBuffer.asParser(jsonFactory.getCodec()).readValueAs(typeReference);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    try {
                        inputFeeder.endOfInput();
                        parser.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, executorService));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).get();
    }

    @Test
    public void test_single_thread_OK() throws IOException, ExecutionException, InterruptedException {
        Map<String, Map<String, String>> tags = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            Map<String, String> value = new HashMap<>();
            for (int j = 0; j < 10; j++) {
                value.put("key_" + j, "val" + j);
            }
            tags.put("elt_" + i, value);
        }

        JsonFactory jsonFactory = new SmileFactory();
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectWriter objectWriter = objectMapper.writer().with(jsonFactory);
        jsonFactory.setCodec(objectMapper);
        byte[] json = objectWriter.writeValueAsBytes(tags);
        TypeReference<Map<String, Map<String, String>>> typeReference = new TypeReference<Map<String, Map<String, String>>>() {
        };

        ExecutorService executorService = Executors.newFixedThreadPool(1);
        List<CompletableFuture<?>> futures = new ArrayList<>();

        int count = 100000;
        for (int i = 0; i < count; i++) {
            JsonParser parser = jsonFactory.createNonBlockingByteArrayParser();
            ByteArrayFeeder inputFeeder = (ByteArrayFeeder) parser.getNonBlockingInputFeeder();
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    inputFeeder.feedInput(json, 0, json.length);
                    TokenBuffer tokenBuffer = new TokenBuffer(parser);
                    while (true) {
                        JsonToken token = parser.nextToken();
                        if (token == JsonToken.NOT_AVAILABLE || token == null) {
                            break;
                        }

                        tokenBuffer.copyCurrentEvent(parser);
                    }
                    return tokenBuffer.asParser(jsonFactory.getCodec()).readValueAs(typeReference);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    try {
                        inputFeeder.endOfInput();
                        parser.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, executorService));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).get();
    }

    @Test
    public void test_json_OK() throws IOException, ExecutionException, InterruptedException {
        Map<String, Map<String, String>> tags = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            Map<String, String> value = new HashMap<>();
            for (int j = 0; j < 10; j++) {
                value.put("key_" + j, "val" + j);
            }
            tags.put("elt_" + i, value);
        }

        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectWriter objectWriter = objectMapper.writer().with(jsonFactory);
        jsonFactory.setCodec(objectMapper);
        byte[] json = objectWriter.writeValueAsBytes(tags);
        TypeReference<Map<String, Map<String, String>>> typeReference = new TypeReference<Map<String, Map<String, String>>>() {
        };

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        List<CompletableFuture<?>> futures = new ArrayList<>();

        int count = 100000;
        for (int i = 0; i < count; i++) {
            JsonParser parser = jsonFactory.createNonBlockingByteArrayParser();
            ByteArrayFeeder inputFeeder = (ByteArrayFeeder) parser.getNonBlockingInputFeeder();
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    inputFeeder.feedInput(json, 0, json.length);
                    TokenBuffer tokenBuffer = new TokenBuffer(parser);
                    while (true) {
                        JsonToken token = parser.nextToken();
                        if (token == JsonToken.NOT_AVAILABLE || token == null) {
                            break;
                        }

                        tokenBuffer.copyCurrentEvent(parser);
                    }
                    return tokenBuffer.asParser(jsonFactory.getCodec()).readValueAs(typeReference);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    try {
                        inputFeeder.endOfInput();
                        parser.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, executorService));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).get();
    }

    @Test
    public void test_same_thread_OK() throws IOException, ExecutionException, InterruptedException {
        Map<String, Map<String, String>> tags = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            Map<String, String> value = new HashMap<>();
            for (int j = 0; j < 10; j++) {
                value.put("key_" + j, "val" + j);
            }
            tags.put("elt_" + i, value);
        }

        JsonFactory jsonFactory = new SmileFactory();
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectWriter objectWriter = objectMapper.writer().with(jsonFactory);
        jsonFactory.setCodec(objectMapper);
        byte[] json = objectWriter.writeValueAsBytes(tags);
        TypeReference<Map<String, Map<String, String>>> typeReference = new TypeReference<Map<String, Map<String, String>>>() {
        };

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        List<CompletableFuture<?>> futures = new ArrayList<>();

        int count = 100000;
        for (int i = 0; i < count; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                JsonParser parser = null;
                ByteArrayFeeder inputFeeder = null;
                try {
                    parser = jsonFactory.createNonBlockingByteArrayParser();
                    inputFeeder = (ByteArrayFeeder) parser.getNonBlockingInputFeeder();
                    inputFeeder.feedInput(json, 0, json.length);
                    TokenBuffer tokenBuffer = new TokenBuffer(parser);
                    while (true) {
                        JsonToken token = parser.nextToken();
                        if (token == JsonToken.NOT_AVAILABLE || token == null) {
                            break;
                        }

                        tokenBuffer.copyCurrentEvent(parser);
                    }
                    return tokenBuffer.asParser(jsonFactory.getCodec()).readValueAs(typeReference);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    try {
                        inputFeeder.endOfInput();
                        parser.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, executorService));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).get();
    }

}