package com.liveaction.reactiff.codec.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.json.async.NonBlockingJsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Charsets.UTF_8;

public final class JsonCodec implements Codec {

    public static final String APPLICATION_STREAM_JSON = "application/stream+json";
    public static final String APPLICATION_JSON = "application/json";

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonCodec.class);

    private static final TypeToken<Mono> MONO_TYPE_TOKEN = TypeToken.of(Mono.class);
    private static final char NEW_LINE_SEPARATOR = '\n';
    private static final char START_ARRAY = '[';
    private static final char COMMA_SEPARATOR = ',';
    private static final byte[] END_ARRAY = "]".getBytes(UTF_8);

    private ObjectMapper objectMapper;

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public int rank() {
        return 1;
    }

    @Override
    public boolean supports(String contentType, TypeToken<?> typeToken) {
        return contentType.contains(APPLICATION_JSON) || contentType.equals(APPLICATION_STREAM_JSON);
    }

    @Override
    public <T> Mono<T> decodeMono(String contentType, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        return ByteBufFlux.fromInbound(byteBufFlux)
                .aggregate()
                .asInputStream()
                .map(inputStream -> {
                    try {
                        return objectMapper.readValue(inputStream, toTypeReference(typeToken));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public static class JsonAsyncParser<T> implements Closeable {

        private final boolean readTopLevelArray;
        private final ObjectMapper objectMapper;
        private final NonBlockingJsonParser parser;
        private final TypeToken<T> typeToken;

        boolean rootLevelArrayStarted = false;
        boolean rootLevelArrayClosed = false;

        public static <T> JsonAsyncParser<T> of(ObjectMapper objectMapper, boolean readTopLevelArray, TypeToken<T> typeToken) throws IOException {
            return new JsonAsyncParser<>(objectMapper, readTopLevelArray, typeToken);
        }

        public JsonAsyncParser(ObjectMapper objectMapper, boolean readTopLevelArray, TypeToken<T> typeToken) throws IOException {
            this.objectMapper = objectMapper;
            this.readTopLevelArray = readTopLevelArray;
            this.typeToken = typeToken;
            this.parser = (NonBlockingJsonParser) objectMapper.getFactory().createNonBlockingByteArrayParser();
        }

        Flux<T> parse(byte[] bytes) {
            try {
                TokenBuffer tokenBuffer = parseTokens(parser, bytes);
                if (tokenBuffer.firstToken() != null) {
                    try {
                        JsonParser jsonParser = tokenBuffer.asParser(objectMapper);
                        ImmutableList<T> objects = ImmutableList.copyOf(() -> {
                            try {
                                return jsonParser.readValuesAs(toTypeReference(typeToken));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                        return Flux.fromIterable(objects);
                    } catch (Throwable e) {
                        return Flux.error(e);
                    }
                } else {
                    return Flux.empty();
                }

            } catch (IOException e) {
                return Flux.error(e);
            }
        }

        int inArray = 0;

        private TokenBuffer parseTokens(NonBlockingJsonParser parser, byte[] array) throws IOException {
            parser.feedInput(array, 0, array.length);
            TokenBuffer tokenBuffer = new TokenBuffer(parser);
            while (true) {
                JsonToken token = this.parser.nextToken();

                if ((token == JsonToken.NOT_AVAILABLE) || token == null) {
                    break;
                }

                if (token == JsonToken.START_ARRAY) {
                    if (readTopLevelArray && !rootLevelArrayStarted) {
                        rootLevelArrayStarted = true;
                        parser.clearCurrentToken();
                        continue;
                    }
                    inArray++;
                } else if (token == JsonToken.END_ARRAY) {
                    if (readTopLevelArray && rootLevelArrayStarted && !rootLevelArrayClosed && inArray == 0) {
                        rootLevelArrayClosed = true;
                        parser.clearCurrentToken();
                        break;
                    }
                    inArray--;
                }
                tokenBuffer.copyCurrentEvent(parser);
            }
            return tokenBuffer;
        }

        @Override
        public void close() {
            try {
                parser.endOfInput();
                parser.close();
            } catch (IOException e) {
                LOGGER.error("Erro while closing parser", e);
            }
        }

    }

    @Override
    public <T> Flux<T> decodeFlux(String contentType, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        if (APPLICATION_JSON.equalsIgnoreCase(contentType)) {

            try {
                JsonAsyncParser<T> jsonAsyncParser = JsonAsyncParser.of(objectMapper, true, typeToken);
                return ByteBufFlux.fromInbound(byteBufFlux)
                        .asByteArray()
                        .flatMap(jsonAsyncParser::parse)
                        .doOnTerminate(jsonAsyncParser::close);
            } catch (IOException e) {
                return Flux.error(e);
            }
        } else if (APPLICATION_STREAM_JSON.equalsIgnoreCase(contentType)) {
            return ByteBufFlux.fromInbound(byteBufFlux)
                    .asInputStream()
                    .map(inputStream -> {
                        try {
                            return objectMapper.readValue(inputStream, toTypeReference(typeToken));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } else {
            throw new IllegalArgumentException("Unsupported ContentType '" + contentType + "'");
        }
    }

    @Override
    public <T> Publisher<ByteBuf> encode(String contentType, Publisher<T> data, TypeToken<T> typeToken) {
        if (APPLICATION_JSON.equalsIgnoreCase(contentType) || APPLICATION_STREAM_JSON.equalsIgnoreCase(contentType)) {
            if (MONO_TYPE_TOKEN.isAssignableFrom(data.getClass())) {
                return Mono.from(data)
                        .map(t -> encodeValue(t, null));
            } else {
                AtomicBoolean first = new AtomicBoolean(true);
                if (APPLICATION_JSON.equalsIgnoreCase(contentType)) {
                    return Flux.from(data)
                            .flatMap(t -> {
                                        if (first.getAndSet(false)) {
                                            return Mono.just(encodeValue(t, START_ARRAY));
                                        } else {
                                            return Mono.just(encodeValue(t, COMMA_SEPARATOR));
                                        }
                                    },
                                    Mono::error,
                                    () -> Mono.just(Unpooled.wrappedBuffer(END_ARRAY)));

                } else {
                    return Flux.from(data)
                            .map(t -> {
                                if (first.getAndSet(false)) {
                                    return encodeValue(t, NEW_LINE_SEPARATOR);
                                } else {
                                    return encodeValue(t, null);
                                }
                            });
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported ContentType '" + contentType + "'");
        }
    }

    private <T> ByteBuf encodeValue(T t, Character before) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            if (before != null) {
                byteArrayOutputStream.write(before);
            }
            objectMapper.writeValue(byteArrayOutputStream, t);
            return Unpooled.wrappedBuffer(byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to encode as JSON", e);
        }
    }

    private static <T> TypeReference<T> toTypeReference(TypeToken<T> typeToken) {
        return new TypeReference<T>() {
            @Override
            public Type getType() {
                return typeToken.getType();
            }
        };
    }

}
