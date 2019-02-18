package com.liveaction.reactiff.codec;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.Codec;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;

import java.nio.Buffer;

public final class RawBinaryCodec implements Codec {

    private static final TypeToken<byte[]> BYTE_ARRAY = TypeToken.of(byte[].class);
    private static final TypeToken<ByteBuf> BYTE_BUFF = TypeToken.of(ByteBuf.class);
    private static final TypeToken<Buffer> BUFFER_TYPE_TOKEN = TypeToken.of(Buffer.class);
    private static final ImmutableSet<TypeToken<?>> BINARY_DATA = ImmutableSet.of(BYTE_ARRAY, BYTE_BUFF, BUFFER_TYPE_TOKEN);

    @Override
    public int rank() {
        return -10;
    }

    @Override
    public boolean supports(String contentType, TypeToken<?> typeToken) {
        return BINARY_DATA.stream().anyMatch(t -> t.isAssignableFrom(typeToken));
    }

    @Override
    public <T> Mono<T> decodeMono(String contentType, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        return Mono.from(decode(byteBufFlux, typeToken));
    }

    @Override
    public <T> Flux<T> decodeFlux(String contentType, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        return Flux.from(decode(byteBufFlux, typeToken));
    }

    @SuppressWarnings("unchecked")
    private <T> Publisher<T> decode(Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        if (BYTE_ARRAY.isAssignableFrom(typeToken)) {
            return (Publisher<T>) ByteBufFlux.fromInbound(byteBufFlux).map(ByteBuf::array);
        } else if (BYTE_BUFF.isAssignableFrom(typeToken)) {
            return (Publisher<T>) ByteBufFlux.fromInbound(byteBufFlux);
        } else if (BUFFER_TYPE_TOKEN.isAssignableFrom(typeToken)) {
            return (Publisher<T>) ByteBufFlux.fromInbound(byteBufFlux).map(ByteBuf::nioBuffer);
        } else {
            throw new IllegalArgumentException("Unable to encode to type '" + typeToken + "'");
        }
    }

    @Override
    public <T> Publisher<ByteBuf> encode(String contentType, Publisher<T> data, TypeToken<T> typeToken) {
        if (BYTE_ARRAY.isAssignableFrom(typeToken)) {
            return ByteBufFlux.fromInbound(data);
        } else if (BYTE_BUFF.isAssignableFrom(typeToken)) {
            return ByteBufFlux.fromInbound(data);
        } else if (BUFFER_TYPE_TOKEN.isAssignableFrom(typeToken)) {
            return ByteBufFlux.fromInbound(data);
        } else {
            throw new IllegalArgumentException("Unable to encode type '" + typeToken + "'");
        }
    }

}
