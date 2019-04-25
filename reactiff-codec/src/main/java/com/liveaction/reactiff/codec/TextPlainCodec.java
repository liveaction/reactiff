package com.liveaction.reactiff.codec;

import com.google.common.base.Charsets;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.Codec;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;

public final class TextPlainCodec implements Codec {

    @Override
    public int rank() {
        return 0;
    }

    @Override
    public boolean supports(String contentType, TypeToken<?> typeToken) {
        return typeToken != null && String.class.equals(typeToken.getRawType()) && contentType.toUpperCase().startsWith("TEXT/");
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
        if (String.class.equals(typeToken.getRawType())) {
            return (Publisher<T>) ByteBufFlux.fromInbound(byteBufFlux).asString();
        } else {
            throw new IllegalArgumentException("Unable to decode to type '" + typeToken + "'. Only string supported");
        }
    }

    @Override
    public <T> Publisher<ByteBuf> encode(String contentType, Publisher<T> data, TypeToken<T> typeToken) {
        if (String.class.equals(typeToken.getRawType())) {
            return ByteBufFlux.fromInbound(Flux.from(data).map(t -> t.toString().getBytes(Charsets.UTF_8)));
        } else {
            throw new IllegalArgumentException("Unable to encode from type '" + typeToken + "'. Only string supported");
        }
    }

}
