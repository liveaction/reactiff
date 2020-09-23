package com.liveaction.reactiff.codec.jackson;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.util.function.Tuples;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;

public class JacksonCodec {

    private static final Logger LOGGER = LoggerFactory.getLogger(JacksonCodec.class);

    private static final TypeToken<Mono> MONO_TYPE_TOKEN = TypeToken.of(Mono.class);

    private JsonFactory jsonFactory;
    private ObjectWriter objectWriter;
    private DeserializerWrapper deserializerWrapper;


    public JacksonCodec(ObjectMapper objectMapper, JsonFactory jsonFactory) {
        this.jsonFactory = jsonFactory;
        reloadMapper(objectMapper);
    }

    public JacksonCodec withDeserializerWrapper(DeserializerWrapper deserializerWrapper) {
        this.deserializerWrapper = deserializerWrapper;
        return this;
    }

    public void reloadMapper(ObjectMapper objectMapper) {
        this.objectWriter = objectMapper.writer().with(jsonFactory);
        jsonFactory.setCodec(objectMapper);
    }

    public <T> Mono<T> decodeMono(Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        return ByteBufFlux.fromInbound(byteBufFlux)
                .aggregate()
                .asByteArray()// https://github.com/reactor/reactor-netty/issues/746
                .map(bytes -> deserialize(() -> jsonFactory.createParser(bytes).readValueAs(toTypeReference(typeToken))));
    }

    public <T> Flux<T> decodeFlux(Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken, boolean readTopLevelArray) {
        try {
            JsonAsyncParser<T> jsonAsyncParser = new JsonAsyncParser<>(jsonFactory, readTopLevelArray, typeToken, this::deserialize);
            return ByteBufFlux.fromInbound(byteBufFlux)
                    .asByteArray()
                    .flatMapIterable(jsonAsyncParser::parse)
                    .doOnTerminate(jsonAsyncParser::close);
        } catch (IOException e) {
            return Flux.error(e);
        }
    }

    public <T> Publisher<ByteBuf> encode(Publisher<T> data, boolean tokenizeArrayElements) {
        if (MONO_TYPE_TOKEN.isSupertypeOf(data.getClass())) {
            return Mono.from(data)
                    .flatMap(obj -> {
                        try {

                            return Mono.just(Unpooled.wrappedBuffer(objectWriter.writeValueAsBytes(obj)));
                        } catch (JsonProcessingException e) {
                            LOGGER.error(String.format("Error in serialization: %s could not be serialized because %s", obj, e.getMessage()));
                            LOGGER.debug(String.format("Error in serialization: %s could not be serialized", obj), e);
                            return Mono.error(e);
                        }
                    });
        } else {
            return encodeValue(Flux.from(data), tokenizeArrayElements);
        }
    }

    private <T> Flux<ByteBuf> encodeValue(Flux<T> values, boolean wrapInArray) {
        return Flux.using(() -> {
                    ByteArrayBuilder output = new ByteArrayBuilder();
                    SequenceWriter sequenceWriter = objectWriter.writeValues(output);
                    sequenceWriter.init(wrapInArray);
                    return Tuples.of(output, sequenceWriter);
                }, tuple -> {
                    ByteArrayBuilder output = tuple.getT1();
                    SequenceWriter sequenceWriter = tuple.getT2();
                    return values.concatMap(val -> {
                        try {
                            output.reset();
                            sequenceWriter.write(val);
                            return Mono.just(Unpooled.wrappedBuffer(output.toByteArray()));
                        } catch (IOException e) {
                            return Mono.error(e);
                        }
                    }).concatWith(Mono.fromCallable(() -> {
                        output.reset();
                        // used to close() the SequenceWriter in implicit finally clause
                        try (SequenceWriter c = tuple.getT2()) {
                        }
                        byte[] array = output.toByteArray();
                        return array.length == 0 ? null : Unpooled.wrappedBuffer(array);
                    }));

                },
                tuple -> {
                    try (ByteArrayBuilder a = tuple.getT1(); SequenceWriter c = tuple.getT2()) {
                    } catch (IOException e) {
                        LOGGER.error("Error when closing resources", e);
                    }
                });
    }

    private <T> T deserialize(Callable<T> callable) {
        T res;
        try {
            Optional.ofNullable(deserializerWrapper).ifPresent(DeserializerWrapper::apply);
            res = callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Optional.ofNullable(deserializerWrapper).ifPresent(DeserializerWrapper::unapply);
        }
        return res;
    }

    private static <T> TypeReference<T> toTypeReference(TypeToken<T> typeToken) {
        return new TypeReference<T>() {
            @Override
            public Type getType() {
                return typeToken.getType();
            }
        };
    }

    public static class JsonAsyncParser<T> implements Closeable {

        private final JsonFactory jsonFactory;
        private final boolean readTopLevelArray;
        private final JsonParser parser;
        private final Function<Callable<T>, T> deserializerFunction;
        private final TypeToken<T> typeToken;
        private TokenBuffer tokenBuffer;

        boolean rootLevelArrayStarted = false;
        boolean rootLevelArrayClosed = false;

        private int objectDepth = 0;
        private int arrayDepth = 0;

        // TODO: change to ByteBufferFeeder when supported by Jackson
        // See https://github.com/FasterXML/jackson-core/issues/478
        private final ByteArrayFeeder inputFeeder;

        public JsonAsyncParser(JsonFactory jsonFactory, boolean readTopLevelArray, TypeToken<T> typeToken, Function<Callable<T>, T> deserializerFunction) throws IOException {
            this.jsonFactory = jsonFactory;
            this.readTopLevelArray = readTopLevelArray;
            this.typeToken = typeToken;
            this.parser = jsonFactory.createNonBlockingByteArrayParser();
            this.deserializerFunction = deserializerFunction;
            this.inputFeeder = (ByteArrayFeeder) this.parser.getNonBlockingInputFeeder();

            this.tokenBuffer = new TokenBuffer(this.parser);
        }


        Collection<T> parse(byte[] bytes) {
            try {
                List<TokenBuffer> tokenBuffers = parseTokens(parser, bytes);
                return tokenBuffers.stream()
                        .map(this::parseTokenBuffer)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(ImmutableList.toImmutableList());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private Optional<T> parseTokenBuffer(TokenBuffer tokenBuffer) {
            Optional<T> res;
            if (tokenBuffer.firstToken() != null) {
                JsonParser jsonParser = tokenBuffer.asParser(jsonFactory.getCodec());
                res = Optional.of(deserializerFunction.apply(() -> jsonParser.readValueAs(toTypeReference(typeToken))));
            } else {
                res = Optional.empty();
            }
            return res;
        }

        private void updateDepth(JsonToken token) {
            switch (token) {
                case START_OBJECT:
                    this.objectDepth++;
                    break;
                case END_OBJECT:
                    this.objectDepth--;
                    break;
                case START_ARRAY:
                    this.arrayDepth++;
                    break;
                case END_ARRAY:
                    this.arrayDepth--;
                    break;
            }
        }

        private List<TokenBuffer> parseTokens(JsonParser parser, byte[] array) throws IOException {
            List<TokenBuffer> result = new ArrayList<>();
            inputFeeder.feedInput(array, 0, array.length);
            while (true) {
                JsonToken token = this.parser.nextToken();

                if (token == JsonToken.NOT_AVAILABLE) {
                    break;
                }
                if (token == null) {
                    token = this.parser.nextToken();
                    if (token == null || token == JsonToken.NOT_AVAILABLE) {
                        break;
                    }
                }
                updateDepth(token);

                if (token == JsonToken.START_ARRAY) {
                    if (readTopLevelArray && !rootLevelArrayStarted && objectDepth == 0) {
                        rootLevelArrayStarted = true;
                        parser.clearCurrentToken();
                        continue;
                    }
                } else if (token == JsonToken.END_ARRAY) {
                    if (readTopLevelArray && rootLevelArrayStarted && !rootLevelArrayClosed && arrayDepth == 0 && objectDepth == 0) {
                        rootLevelArrayClosed = true;
                        parser.clearCurrentToken();
                        break;
                    }
                }
                tokenBuffer.copyCurrentEvent(parser);
                if (this.objectDepth == 0 && (this.arrayDepth == 0 || (this.arrayDepth == 1 && readTopLevelArray))) {
                    result.add(tokenBuffer);
                    tokenBuffer = new TokenBuffer(parser);
                }
            }
            return result;
        }

        @Override
        public void close() {
            try {
                inputFeeder.endOfInput();
                parser.close();
            } catch (IOException e) {
                LOGGER.error("Error while closing parser", e);
            }
        }
    }

}
