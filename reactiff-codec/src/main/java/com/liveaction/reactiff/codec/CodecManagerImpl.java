package com.liveaction.reactiff.codec;

import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.Codec;
import com.liveaction.reactiff.api.codec.CodecManager;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public final class CodecManagerImpl implements CodecManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodecManagerImpl.class);
    private static final String DEFAULT = "application/json";

    private final Set<Codec> codecs = new ConcurrentSkipListSet<>(Comparator.comparingInt(Codec::rank));

    private String defaultContentType = DEFAULT;

    @Override
    public void addCodec(Codec codec) {
        boolean add = codecs.add(codec);
        if (!add) {
            LOGGER.warn("Codec rank conflict : {} (rank = {}). It has not been added. Fix this !", codec, codec.rank());
        }
    }

    @Override
    public void removeCodec(Codec codec) {
        codecs.add(codec);
    }

    @Override
    public void setDefaultContentType(String defaultContentType) {
        this.defaultContentType = defaultContentType;
    }

    @Override
    public <T> Mono<T> decodeAsMono(HttpHeaders httpHeaders, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        String contentType = findContentType(httpHeaders);
        Codec codec = findCodec(contentType, typeToken);
        return codec.decodeMono(contentType, byteBufFlux, typeToken);
    }

    @Override
    public <T> Mono<T> decodeAsMono(HttpServerRequest request, TypeToken<T> typeToken) {
        return decodeAsMono(request.requestHeaders(), request.receive(), typeToken);
    }

    @Override
    public <T> Flux<T> decodeAsFlux(HttpHeaders httpHeaders, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        String contentType = findContentType(httpHeaders);
        Codec codec = findCodec(contentType, typeToken);
        return codec.decodeFlux(contentType, byteBufFlux, typeToken);
    }

    @Override
    public <T> Flux<T> decodeAsFlux(HttpServerRequest request, TypeToken<T> typeToken) {
        return decodeAsFlux(request.requestHeaders(), request.receive(), typeToken);
    }

    private String findContentType(HttpHeaders httpHeaders) {
        return Optional.ofNullable(httpHeaders.get(HttpHeaderNames.CONTENT_TYPE))
                .orElse(defaultContentType);
    }

    @Override
    public <T> Publisher<ByteBuf> encode(HttpHeaders requestHttpHeaders, HttpHeaders responseHttpHeaders, Publisher<T> data, TypeToken<T> typeToken) {
        String contentType = responseHttpHeaders.get(HttpHeaderNames.CONTENT_TYPE);
        if (contentType == null) {
            String acceptHeader = requestHttpHeaders.get(HttpHeaderNames.ACCEPT);
            contentType = negociateContentType(acceptHeader, typeToken);
        }
        return encodeAs(contentType, responseHttpHeaders, data, typeToken);
    }

    @Override
    public <T> Publisher<ByteBuf> encodeAs(String contentType, HttpHeaders httpHeaders, Publisher<T> data, TypeToken<T> typeToken) {
        LOGGER.debug("Found an encoder for Content-Type='{}'", contentType);
        httpHeaders.set(HttpHeaderNames.CONTENT_TYPE, contentType);
        return encodeAs(contentType, data, typeToken);
    }

    @Override
    public <T> Publisher<ByteBuf> encodeAs(String contentType, Publisher<T> data, TypeToken<T> typeToken) {
        Codec codec = codecs.stream()
                .filter(myCodec -> myCodec.supports(contentType, typeToken))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unable to found an encoder that supports Content-Type '" + contentType + "' and type '" + typeToken + "'"));
        try {
            return codec.encode(contentType, data, typeToken);
        } catch (Exception e) {
            return Flux.error(e);
        }
    }

    private String negociateContentType(String acceptHeader, TypeToken<?> typeToken) {
        for (String contentType : acceptHeader.split(",")) {
            String trim = contentType.trim();
            Optional<String> matches = codecs.stream()
                    .map(codec -> {
                        if (codec.supports(trim, typeToken)) {
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
        return defaultContentType;
    }

    private Codec findCodec(String contentType, TypeToken<?> typeToken) {
        Codec codec = codecs.stream()
                .filter(myCodec -> myCodec.supports(contentType, typeToken))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unable to found a decoder that supports Content-Type '" + contentType + "' and type '" + typeToken + "'"));
        LOGGER.debug("Found a decoder for Content-Type='{}'", contentType);
        return codec;
    }

}
