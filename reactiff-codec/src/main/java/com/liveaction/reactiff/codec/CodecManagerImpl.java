package com.liveaction.reactiff.codec;

import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.Codec;
import com.liveaction.reactiff.api.codec.CodecManager;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.http.server.HttpServerRequest;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public final class CodecManagerImpl implements CodecManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodecManagerImpl.class);
    private static final String TEXT_PLAIN = "text/plain";

    private final Set<Codec> codecs = new ConcurrentSkipListSet<>(Comparator.comparingInt(Codec::rank));

    @Override
    public void addCodec(Codec codec) {
        codecs.add(codec);
    }

    @Override
    public void removeCodec(Codec codec) {
        codecs.add(codec);
    }

    @Override
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

    @Override
    public <T> Publisher<T> decodeAs(HttpServerRequest request, TypeToken<T> typeToken) {
        String contentType = Optional.ofNullable(request.requestHeaders().get(HttpHeaderNames.CONTENT_TYPE))
                .orElse(TEXT_PLAIN);
        Codec codec = codecs.stream()
                .filter(myCodec -> myCodec.supports(contentType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unable to found a decoder that supports Content-Type '" + contentType + "'"));
        LOGGER.debug("Found a decoder for Content-Type='{}'", contentType);
        return codec.decode(contentType, request.receive(), typeToken);
    }

    @Override
    public <T> Publisher<ByteBuf> encode(HttpHeaders requestHttpHeaders, HttpHeaders responseHttpHeaders, Publisher<T> data) {
        String acceptHeader = requestHttpHeaders.get(HttpHeaderNames.ACCEPT);
        String contentType = negociateContentType(acceptHeader);
        return encodeAs(contentType, responseHttpHeaders, data);
    }

    @Override
    public <T> Publisher<ByteBuf> encodeAs(String contentType, HttpHeaders httpHeaders, Publisher<T> data) {
        LOGGER.debug("Found an encoder for Content-Type='{}'", contentType);
        httpHeaders.set(HttpHeaderNames.CONTENT_TYPE, contentType);
        return encodeAs(contentType, data);
    }

    @Override
    public <T> Publisher<ByteBuf> encodeAs(String contentType, Publisher<T> data) {
        Codec codec = codecs.stream()
                .filter(myCodec -> myCodec.supports(contentType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unable to found an encoder that supports Content-Type '" + contentType + "'"));
        return codec.encode(contentType, data);
    }

    private String negociateContentType(String acceptHeader) {
        for (String contentType : acceptHeader.split(",")) {
            String trim = contentType.trim();
            Optional<String> matches = codecs.stream()
                    .map(codec -> {
                        if (codec.supports(trim)) {
                            return trim;
                        } else {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .findFirst();
            if (matches.isPresent()) {
                return matches.get();
            }
        }
        return HttpHeaderValues.TEXT_PLAIN.toString();
    }

}
