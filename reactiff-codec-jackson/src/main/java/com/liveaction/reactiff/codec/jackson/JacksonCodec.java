package com.liveaction.reactiff.codec.jackson;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public final class JacksonCodec {

    private static final Logger LOGGER = LoggerFactory.getLogger(JacksonCodec.class);

    private static final TypeToken<Mono> MONO_TYPE_TOKEN = TypeToken.of(Mono.class);
    private static final byte[] START_ARRAY = {'['};
    private static final byte[] COMMA_SEPARATOR = {','};
    private static final byte[] END_ARRAY = {']'};

    private final ObjectCodec objectCodec;
    private final JsonFactory jsonFactory;
    private final byte[] streamSeparator;

    public JacksonCodec(ObjectCodec objectCodec, JsonFactory jsonFactory, byte[] streamSeparator) {
        this.objectCodec = objectCodec;
        this.jsonFactory = jsonFactory.setCodec(objectCodec);
        this.streamSeparator = streamSeparator;
    }

    public <T> Mono<T> decodeMono(Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        return ByteBufFlux.fromInbound(byteBufFlux)
                .aggregate()
                .asInputStream()
                .map(inputStream -> {
                    try {
                        return objectCodec.readValue(jsonFactory.createParser(inputStream), toTypeReference(typeToken));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public <T> Flux<T> decodeFlux(Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken, boolean readTopLevelArray) {
        try {
            JsonAsyncParser<T> jsonAsyncParser = new JsonAsyncParser<>(objectCodec, jsonFactory, readTopLevelArray, typeToken);
            return ByteBufFlux.fromInbound(byteBufFlux)
                    .asByteArray()
                    .flatMap(jsonAsyncParser::parse)
                    .doOnTerminate(jsonAsyncParser::close);
        } catch (IOException e) {
            return Flux.error(e);
        }
    }

    public <T> Publisher<ByteBuf> encode(Publisher<T> data, boolean tokenizeArrayElements) {
        if (MONO_TYPE_TOKEN.isAssignableFrom(data.getClass())) {
            return encodeValue(Flux.from(data), () -> null, () -> null);
        } else {
            AtomicBoolean first = new AtomicBoolean(true);
            if (tokenizeArrayElements) {
                return Flux.from(data)
                        .transform(f -> encodeValue(f, () -> first.getAndSet(false) ? START_ARRAY : COMMA_SEPARATOR, () -> null))
                        .concatWith(Mono.defer(() -> first.get() ? Mono.empty() : Mono.just(Unpooled.wrappedBuffer(END_ARRAY))));

            } else {
                return Flux.from(data)
                        .transform(f -> encodeValue(f, () -> null, () -> streamSeparator));
            }
        }
    }

    private <T> Flux<ByteBuf> encodeValue(Flux<T> valuess, Supplier<byte[]> beforeSupp, Supplier<byte[]> afterSupp) {
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();
        ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(byteBuf);
        JsonGenerator generator;
        try {
            generator = jsonFactory.createGenerator((OutputStream) byteBufOutputStream);
        } catch (IOException e) {
            return Flux.error(e);
        }
        return valuess.concatMap(val -> {
            try {
                byteBuf.clear();
                byte[] before = beforeSupp.get();
                if (before != null) {
                    byteBufOutputStream.write(before);
                }

                objectCodec.writeValue(generator, val);
                generator.flush();
                byte[] after = afterSupp.get();
                if (after != null) {
                    byteBufOutputStream.write(after);
                }
                return Mono.just(byteBuf.copy());
            } catch (IOException e) {
                return Mono.error(e);
            }
        });
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

        private final boolean readTopLevelArray;
        private final JsonParser parser;
        private final TypeToken<T> typeToken;
        private final ObjectCodec objectCodec;
        private TokenBuffer tokenBuffer;

        boolean rootLevelArrayStarted = false;
        boolean rootLevelArrayClosed = false;

        private int objectDepth = 0;
        private int arrayDepth = 0;

        // TODO: change to ByteBufferFeeder when supported by Jackson
        // See https://github.com/FasterXML/jackson-core/issues/478
        private final ByteArrayFeeder inputFeeder;

        public JsonAsyncParser(ObjectCodec objectCodec, JsonFactory jsonFactory, boolean readTopLevelArray, TypeToken<T> typeToken) throws IOException {
            this.objectCodec = objectCodec;
            this.readTopLevelArray = readTopLevelArray;
            this.typeToken = typeToken;
            this.parser = jsonFactory.createNonBlockingByteArrayParser();
            this.inputFeeder = (ByteArrayFeeder) this.parser.getNonBlockingInputFeeder();

            this.tokenBuffer = new TokenBuffer(this.parser);
        }


        Flux<T> parse(byte[] bytes) {
            try {
                List<TokenBuffer> tokenBuffers = parseTokens(parser, bytes);
                return Flux.fromIterable(tokenBuffers)
                        .flatMapIterable(tokenBuffer -> {
                            if (tokenBuffer.firstToken() != null) {
                                JsonParser jsonParser = tokenBuffer.asParser(objectCodec);
                                return ImmutableList.copyOf(() -> {
                                    try {
                                        return jsonParser.readValuesAs(toTypeReference(typeToken));
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                            } else {
                                return ImmutableList.of();
                            }

                        });
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
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
                LOGGER.error("Erro while closing parser", e);
            }
        }
    }

}
