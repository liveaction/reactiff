package com.liveaction.reactiff.codec;

import com.google.common.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.http.server.HttpServerResponse;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class CodecManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodecManager.class);
    private static final String TEXT_PLAIN = "text/plain";

    private final Set<Codec> codecs = new ConcurrentSkipListSet<>(Comparator.comparingInt(Codec::rank));

    public void addCodec(Codec codec) {
        codecs.add(codec);
    }

    public void removeCodec(Codec codec) {
        codecs.add(codec);
    }

    public <T> BiFunction<HttpClientResponse, ByteBufFlux, Publisher<T>> decodeAs(Class<T> clazz) {
        return decodeAs(TypeToken.of(clazz));
    }

    public <T> BiFunction<HttpClientResponse, ByteBufFlux, Publisher<T>> decodeAs(TypeToken<T> typeToken) {
        return (response, byteBufFlux) -> this.decodeAs(response, byteBufFlux, typeToken);
    }

    public <T> Publisher<T> decodeAs(HttpClientResponse response, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        String contentType = Optional.ofNullable(response.responseHeaders().get(HttpHeaderNames.CONTENT_TYPE))
                .orElse(TEXT_PLAIN);
        Codec codec = codecs.stream()
                .filter(myCodec -> myCodec.supports(contentType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unable to found a decoder that supports Content-Type '" + contentType + "'"));
        LOGGER.debug("Found a decoder for Content-Type='{}'", contentType);
        return codec.decode(contentType, byteBufFlux, typeToken);
    }

    public <T> Publisher<ByteBuf> encode(HttpHeaders httpHeaders, HttpServerResponse response, Publisher<T> data) {
        String s = httpHeaders.get(HttpHeaderNames.ACCEPT);
        String c = Stream.of(s.split(";"))
                .filter(contentType -> codecs.stream()
                        .anyMatch(codec -> codec.supports(contentType)))
                .findFirst()
                .orElse(HttpHeaderValues.TEXT_PLAIN.toString());
        return encodeAs(c, response, data);
    }

    public <T> Publisher<ByteBuf> encodeAs(String contentType, HttpServerResponse response, Publisher<T> data) {
        Codec codec = codecs.stream()
                .filter(myCodec -> myCodec.supports(contentType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unable to found an encoder that supports Content-Type '" + contentType + "'"));
        LOGGER.debug("Found a encoder for Content-Type='{}'", contentType);
        LOGGER.info("set {}", HttpHeaderNames.CONTENT_TYPE);
        response.responseHeaders().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        return codec.encode(contentType, data);
    }

}
