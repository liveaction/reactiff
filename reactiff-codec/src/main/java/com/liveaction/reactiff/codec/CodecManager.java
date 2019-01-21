package com.liveaction.reactiff.codec;

import com.google.common.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import org.reactivestreams.Publisher;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.util.function.BiFunction;

public interface CodecManager {

    default <T> BiFunction<HttpClientResponse, ByteBufFlux, Publisher<T>> decodeAs(Class<T> clazz) {
        return decodeAs(TypeToken.of(clazz));
    }

    default <T> BiFunction<HttpClientResponse, ByteBufFlux, Publisher<T>> decodeAs(TypeToken<T> typeToken) {
        return (response, byteBufFlux) -> this.decodeAs(response, byteBufFlux, typeToken);
    }

    void addCodec(Codec codec);

    void removeCodec(Codec codec);

    <T> Publisher<T> decodeAs(HttpClientResponse response, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken);

    <T> Publisher<T> decodeAs(HttpServerRequest request, TypeToken<T> typeToken);

    <T> Publisher<ByteBuf> encode(HttpHeaders httpHeaders, HttpServerResponse response, Publisher<T> data);

    <T> Publisher<ByteBuf> encodeAs(String contentType, HttpServerResponse response, Publisher<T> data);
}
