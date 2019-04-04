package com.liveaction.reactiff.codec.jackson;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.Codec;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class SmileBinaryCodec implements Codec {

    public static final String APPLICATION_BINARY = "application/octet-stream";

    private final JacksonCodec jacksonCodec;

    public SmileBinaryCodec(ObjectCodec objectCodec) {
        this.jacksonCodec = new JacksonCodec(objectCodec, new SmileFactory(), new byte[0]);
    }

    @Override
    public int rank() {
        return 2;
    }

    @Override
    public boolean supports(String contentType, TypeToken<?> typeToken) {
        return contentType.contains(APPLICATION_BINARY);
    }

    @Override
    public <T> Mono<T> decodeMono(String contentType, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        return jacksonCodec.decodeMono(byteBufFlux, typeToken);
    }

    @Override
    public <T> Flux<T> decodeFlux(String contentType, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        return jacksonCodec.decodeFlux(byteBufFlux, typeToken, false);
    }

    @Override
    public <T> Publisher<ByteBuf> encode(String contentType, Publisher<T> data, TypeToken<T> typeToken) {
        return jacksonCodec.encode(data, false);
    }
}
