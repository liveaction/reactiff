package com.liveaction.reactiff.api.codec;

import com.google.common.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
import reactor.netty.NettyPipeline;
import reactor.netty.http.client.HttpClientRequest;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.http.server.HttpServerRequest;

import java.util.function.BiFunction;

public interface CodecManager {

    default <T> BiFunction<HttpClientResponse, Publisher<ByteBuf>, Mono<T>> decodeAsMono(Class<T> clazz) {
        return decodeAsMono(TypeToken.of(clazz));
    }

    default <T> BiFunction<HttpClientResponse, Publisher<ByteBuf>, Mono<T>> decodeAsMono(TypeToken<T> typeToken) {
        return (response, byteBufFlux) -> decodeAsMono(response, byteBufFlux, typeToken);
    }

    default <T> BiFunction<HttpClientResponse, Publisher<ByteBuf>, Flux<T>> decodeAsFlux(Class<T> clazz) {
        return decodeAsFlux(TypeToken.of(clazz));
    }

    default <T> BiFunction<HttpClientResponse, Publisher<ByteBuf>, Flux<T>> decodeAsFlux(TypeToken<T> typeToken) {
        return (response, byteBufFlux) -> decodeAsFlux(response, byteBufFlux, typeToken);
    }


    default <T> BiFunction<? super HttpClientRequest, ? super NettyOutbound, ? extends Publisher<Void>> send(Body<T> body) {
        return send(body.data, body.type);
    }

    default <T> BiFunction<? super HttpClientRequest, ? super NettyOutbound, ? extends Publisher<Void>> send(String contentType, Publisher<T> data, Class<T> clazz) {
        return send(contentType, data, TypeToken.of(clazz));
    }

    default <T> BiFunction<? super HttpClientRequest, ? super NettyOutbound, ? extends Publisher<Void>> send(String contentType, Body<T> body) {
        return send(contentType, body.data, body.type);
    }

    default <T> BiFunction<? super HttpClientRequest, ? super NettyOutbound, ? extends Publisher<Void>> send(String contentType, Publisher<T> data, TypeToken<T> typeToken) {
        return (httpClientRequest, nettyOutbound) ->
                nettyOutbound.withConnection(connection -> connection.channel().config().setAutoRead(true))
                        .options(NettyPipeline.SendOptions::flushOnEach)
                        .send(encodeAs(contentType, httpClientRequest.requestHeaders(), data, typeToken));
    }

    default <T> Mono<T> decodeAsMono(HttpClientResponse response, Publisher<ByteBuf> byteBufFlux, Class<T> clazz) {
        return decodeAsMono(response, byteBufFlux, TypeToken.of(clazz));
    }

    default <T> Flux<T> decodeAsFlux(HttpClientResponse response, Publisher<ByteBuf> byteBufFlux, Class<T> clazz) {
        return decodeAsFlux(response, byteBufFlux, TypeToken.of(clazz));
    }

    default <T> BiFunction<? super HttpClientRequest, ? super NettyOutbound, ? extends Publisher<Void>> send(Publisher<T> data, TypeToken<T> typeToken) {
        return (httpClientRequest, nettyOutbound) -> {
            String contentType = httpClientRequest.requestHeaders().get("Content-Type");
            if (contentType == null) {
                throw new IllegalArgumentException("No content-type set in http headers. Unable to determine one, please specify one to encode the body");
            }
            return nettyOutbound.send(encodeAs(contentType, data, typeToken));
        };
    }

    void addCodec(Codec codec);

    void removeCodec(Codec codec);

    void setDefaultContentType(String defaultContentType);

    <T> Mono<T> decodeAsMono(HttpClientResponse response, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken);

    <T> Mono<T> decodeAsMono(HttpServerRequest request, TypeToken<T> typeToken);

    <T> Flux<T> decodeAsFlux(HttpClientResponse response, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken);

    <T> Flux<T> decodeAsFlux(HttpServerRequest request, TypeToken<T> typeToken);

    /**
     * Negociate the best matching Content-Type between the requestHttpHeaders and the available codecs.
     * Read 'Accept' header from the requestHttpHeaders.
     * Writes the matching 'Content-Type' to the responseHttpHeaders and returns the data produced by this codec.
     */
    <T> Publisher<ByteBuf> encode(HttpHeaders requestHttpHeaders, HttpHeaders responseHttpHeaders, Publisher<T> data, TypeToken<T> typeToken);

    <T> Publisher<ByteBuf> encodeAs(String contentType, HttpHeaders responseHttpHeaders, Publisher<T> data, TypeToken<T> typeToken);

    <T> Publisher<ByteBuf> encodeAs(String contentType, Publisher<T> data, TypeToken<T> typeToken);

}
