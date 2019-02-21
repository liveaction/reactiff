package com.liveaction.reactiff.codec.binary;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;

public final class SmileBinaryCodec implements Codec {

    public static final String APPLICATION_BINARY = "application/octet-stream";

    private ObjectMapper objectMapper = new ObjectMapper(new SmileFactory());

    public void addModule(Module module) {
        objectMapper.registerModule(module);
    }

    @Override
    public int rank() {
        return 0;
    }

    @Override
    public boolean supports(String contentType, TypeToken<?> typeToken) {
        return contentType.contains(APPLICATION_BINARY);
    }

    @Override
    public <T> Mono<T> decodeMono(String contentType, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        return decodeFlux(contentType, byteBufFlux, typeToken).next();
    }

    @Override
    public <T> Flux<T> decodeFlux(String contentType, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        return ByteBufFlux.fromInbound(byteBufFlux)
                .asInputStream()
                .map(inputStream -> {
                    try {
                        return objectMapper.readValue(inputStream, toTypeReference(typeToken));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
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

    @Override
    public <T> Publisher<ByteBuf> encode(String contentType, Publisher<T> data, TypeToken<T> typeToken) {
        return Flux.from(data)
                .map(t -> {
                    try {
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        objectMapper.writeValue(byteArrayOutputStream, t);
                        return Unpooled.wrappedBuffer(byteArrayOutputStream.toByteArray());
                    } catch (IOException e) {
                        throw new IllegalStateException("Unable to encode as JSON", e);
                    }
                });
    }
}
