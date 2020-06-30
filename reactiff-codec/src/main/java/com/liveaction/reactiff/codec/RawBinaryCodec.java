package com.liveaction.reactiff.codec;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.Unpooled;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.ByteBufMono;

import java.nio.ByteBuffer;
import java.util.function.Function;

public final class RawBinaryCodec implements Codec {

    private static final Logger LOGGER = LoggerFactory.getLogger(RawBinaryCodec.class);

    private static final TypeToken<byte[]> BYTE_ARRAY = TypeToken.of(byte[].class);
    private static final TypeToken<ByteBuf> BYTE_BUFF = TypeToken.of(ByteBuf.class);
    private static final TypeToken<ByteBuffer> BYTE_BUFFER_TYPE_TOKEN = TypeToken.of(ByteBuffer.class);
    private static final ImmutableSet<TypeToken<?>> BINARY_DATA = ImmutableSet.of(BYTE_ARRAY, BYTE_BUFF, BYTE_BUFFER_TYPE_TOKEN);
    private static final TypeToken<Mono> MONO_TYPE_TOKEN = TypeToken.of(Mono.class);

    private final static Function<Object, ByteBuf> bytebufExtractor = o -> {
        if (o instanceof ByteBuf) {
            return (ByteBuf) o;
        }
        if (o instanceof ByteBuffer) {
            return Unpooled.wrappedBuffer((ByteBuffer) o);
        }
        if (o instanceof ByteBufHolder) {
            return ((ByteBufHolder) o).content();
        }
        if (o instanceof byte[]) {
            return Unpooled.wrappedBuffer((byte[]) o);
        }
        throw new IllegalArgumentException("Object " + o + " of type " + o.getClass() + " " + "cannot be converted to ByteBuf");
    };

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
        if (MONO_TYPE_TOKEN.isSupertypeOf(data.getClass())) {
            return Mono.from(data)
                    .map(bytebufExtractor);
        } else {
            return Flux.from(data)
                    .map(bytebufExtractor);
        }
    }
}
