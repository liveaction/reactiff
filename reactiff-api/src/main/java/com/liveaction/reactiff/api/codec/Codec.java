package com.liveaction.reactiff.api.codec;

import com.google.common.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface Codec {

    int rank();

    boolean supports(String contentType, TypeToken<?> typeToken);

    <T> Mono<T> decodeMono(String contentType, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken);

    <T> Flux<T> decodeFlux(String contentType, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken);

    <T> T decodeEntity(String value, TypeToken<T> typeToken);

    <T> Publisher<ByteBuf> encode(String contentType, Publisher<T> data, TypeToken<T> typeToken);

}
