package com.liveaction.reactiff.codec.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.Codec;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class SmileBinaryCodec extends JacksonCodec implements Codec {

    public static final String APPLICATION_BINARY = "application/octet-stream";

    public SmileBinaryCodec(ObjectMapper objectCodec) {
        super(objectCodec, new SmileFactory());
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
        return super.decodeMono(byteBufFlux, typeToken);
    }

    @Override
    public <T> Flux<T> decodeFlux(String contentType, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        return super.decodeFlux(byteBufFlux, typeToken, true);
    }

    @Override
    public <T> Publisher<ByteBuf> encode(String contentType, Publisher<T> data, TypeToken<T> typeToken) {
        return super.encode(data, true);
    }
}
