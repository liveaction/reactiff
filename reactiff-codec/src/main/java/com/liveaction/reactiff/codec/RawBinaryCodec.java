package com.liveaction.reactiff.codec;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.ByteBufMono;

import java.nio.ByteBuffer;

public final class RawBinaryCodec implements Codec {

    private static final TypeToken<byte[]> BYTE_ARRAY = TypeToken.of(byte[].class);
    private static final TypeToken<ByteBuf> BYTE_BUFF = TypeToken.of(ByteBuf.class);
    private static final TypeToken<ByteBuffer> BYTE_BUFFER_TYPE_TOKEN = TypeToken.of(ByteBuffer.class);
    private static final ImmutableSet<TypeToken<?>> BINARY_DATA = ImmutableSet.of(BYTE_ARRAY, BYTE_BUFF, BYTE_BUFFER_TYPE_TOKEN);

    @Override
    public int rank() {
        return -10;
    }

    @Override
    public boolean supports(String contentType, TypeToken<?> typeToken) {
        return typeToken != null && BINARY_DATA.stream().anyMatch(t -> t.isSupertypeOf(typeToken));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Mono<T> decodeMono(String contentType, Publisher<ByteBuf> input, TypeToken<T> typeToken) {
        ByteBufMono byteBufMono = ByteBufFlux.fromInbound(input)
                .aggregate();
        if (BYTE_ARRAY.isSupertypeOf(typeToken)) {
            return (Mono<T>) byteBufMono
                    .asByteArray();
        } else if (BYTE_BUFF.isSupertypeOf(typeToken)) {
            return (Mono<T>) byteBufMono;
        } else if (BYTE_BUFFER_TYPE_TOKEN.isSupertypeOf(typeToken)) {
            return (Mono<T>) byteBufMono
                    .asByteBuffer();
        } else {
            throw new IllegalArgumentException("Unable to encode to type '" + typeToken + "'");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Flux<T> decodeFlux(String contentType, Publisher<ByteBuf> input, TypeToken<T> typeToken) {
        ByteBufFlux byteBufFlux = ByteBufFlux.fromInbound(input);
        if (BYTE_ARRAY.isSupertypeOf(typeToken)) {
            return (Flux<T>) byteBufFlux
                    .asByteArray();
        } else if (BYTE_BUFF.isSupertypeOf(typeToken)) {
            return (Flux<T>) byteBufFlux;
        } else if (BYTE_BUFFER_TYPE_TOKEN.isSupertypeOf(typeToken)) {
            return (Flux<T>) byteBufFlux
                    .asByteBuffer();
        } else {
            throw new IllegalArgumentException("Unable to encode to type '" + typeToken + "'");
        }
    }

    @Override
    public <T> Publisher<ByteBuf> encode(String contentType, Publisher<T> data, TypeToken<T> typeToken) {
        if (BYTE_ARRAY.isSupertypeOf(typeToken)) {
            return ByteBufFlux.fromInbound(data);
        } else if (BYTE_BUFF.isSupertypeOf(typeToken)) {
            return ByteBufFlux.fromInbound(data);
        } else if (BYTE_BUFFER_TYPE_TOKEN.isSupertypeOf(typeToken)) {
            return ByteBufFlux.fromInbound(Flux.from(data)
                    .cast(ByteBuffer.class)
                    .map(Unpooled::wrappedBuffer));
        } else {
            throw new IllegalArgumentException("Unable to encode type '" + typeToken + "'");
        }
    }

}
