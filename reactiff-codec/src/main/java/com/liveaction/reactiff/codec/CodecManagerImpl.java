package com.liveaction.reactiff.codec;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.Codec;
import com.liveaction.reactiff.api.codec.CodecManager;
import com.liveaction.reactiff.api.server.Result;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.StreamSupport;

public final class CodecManagerImpl implements CodecManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodecManagerImpl.class);
    private static final String DEFAULT_CONTENT_TYPE = "application/json";
    private static final String ACCEPT_ALL_HEADER = "*/*";
    private static final ImmutableList<String> DEFAULT_ACCEPT_HEADERS = ImmutableList.of("application/json", "text/plain");

    private final Set<Codec> codecs = new ConcurrentSkipListSet<>(Comparator.comparingInt(Codec::rank));

    private String defaultContentType = DEFAULT_CONTENT_TYPE;

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
        return findCodec(contentType, typeToken)
                .flatMap(codec -> codec.decodeMono(contentType, byteBufFlux, typeToken));
    }

    @Override
    public <T> Mono<T> decodeAsMono(HttpServerRequest request, TypeToken<T> typeToken) {
        return decodeAsMono(request.requestHeaders(), request.receive(), typeToken);
    }

    @Override
    public <T> Flux<T> decodeAsFlux(HttpHeaders httpHeaders, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        String contentType = findContentType(httpHeaders);
        return findCodec(contentType, typeToken)
                .flatMapMany(codec -> codec.decodeFlux(contentType, byteBufFlux, typeToken));
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
    public <T> Mono<Result<T>> enrichResult(HttpHeaders requestHttpHeaders, HttpHeaders responseHttpHeaders, Result<T> result) {
        String contentTypeHeader = responseHttpHeaders.get(HttpHeaderNames.CONTENT_TYPE);
        final String contentType = contentTypeHeader == null ?
                negotiateContentType(getAcceptHeaders(requestHttpHeaders), result.type()) :
                contentTypeHeader;

        return getOptionalCodec(contentType, result.type())
                .map(codec -> codec.enrich(result, contentType))
                .orElse(Mono.just(result));
    }

    private Collection<String> getAcceptHeaders(HttpHeaders requestHttpHeaders) {
        List<String> acceptHeaders = requestHttpHeaders.getAll(HttpHeaderNames.ACCEPT);
        if (acceptHeaders.size() == 1 && acceptHeaders.get(0).equals(ACCEPT_ALL_HEADER)) {
            return DEFAULT_ACCEPT_HEADERS;
        } else {
            return acceptHeaders;
        }
    }

    @Override
    public <T> Publisher<ByteBuf> encode(HttpHeaders requestHttpHeaders, HttpHeaders responseHttpHeaders, Publisher<T> data, TypeToken<T> typeToken) {
        String contentType = responseHttpHeaders.get(HttpHeaderNames.CONTENT_TYPE);
        if (contentType == null) {
            contentType = negotiateContentType(getAcceptHeaders(requestHttpHeaders), typeToken);
        }
        return encodeAs(contentType, responseHttpHeaders, data, typeToken);
    }

    @Override
    public <T> Publisher<ByteBuf> encodeAs(String contentType, HttpHeaders httpHeaders, Publisher<T> data, TypeToken<T> typeToken) {
        Codec codec = getCodec(contentType, typeToken);
        LOGGER.debug("Found an encoder for Content-Type='{}'", contentType);
        httpHeaders.set(HttpHeaderNames.CONTENT_TYPE, contentType);
        return encodeAs(codec, contentType, data, typeToken);
    }

    @Override
    public <T> Publisher<ByteBuf> encodeAs(HttpHeaders requestHttpHeaders, Publisher<T> data, TypeToken<T> typeToken) {
        String contentType = requestHttpHeaders.get(HttpHeaderNames.CONTENT_TYPE);
        if (contentType == null) {
            contentType = negotiateContentType(getAcceptHeaders(requestHttpHeaders), typeToken);
        }
        return encodeAs(getCodec(contentType, typeToken), contentType, data, typeToken);
    }

    private <T> Publisher<ByteBuf> encodeAs(Codec codec, String contentType, Publisher<T> data, TypeToken<T> typeToken) {
        try {
            return codec.encode(contentType, data, typeToken);
        } catch (Exception e) {
            return Flux.error(e);
        }
    }

    private <T> Codec getCodec(String contentType, TypeToken<T> typeToken) {
        return getOptionalCodec(contentType, typeToken)
                .orElseThrow(() -> new IllegalArgumentException("Unable to find an encoder that supports Content-Type '" + contentType + "' and type '" + typeToken + "'"));
    }

    private <T> Optional<Codec> getOptionalCodec(String contentType, TypeToken<T> typeToken) {
        return codecs.stream()
                .filter(myCodec -> myCodec.supports(contentType, typeToken))
                .findFirst();

    }

    private String negotiateContentType(Collection<String> acceptHeader, TypeToken<?> typeToken) {
        return acceptHeader.stream()
                .flatMap(s -> StreamSupport.stream(Splitter.on(',').split(s).spliterator(), false))
                .map(String::trim)
                .map(contentType -> codecs.stream()
                        .map(codec -> {
                            if (codec.supports(contentType, typeToken)) {
                                return Tuples.of(contentType, codec);
                            } else {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .findFirst())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparingInt(tuple -> tuple.getT2().rank()))
                .map(Tuple2::getT1)
                .findFirst()
                .orElse(defaultContentType);
    }

    private Mono<Codec> findCodec(String contentType, TypeToken<?> typeToken) {
        return codecs.stream()
                .filter(myCodec -> myCodec.supports(contentType, typeToken))
                .findFirst()
                .map(Mono::just)
                .orElseGet(() -> Mono.error(new IllegalArgumentException("Unable to find a decoder that supports Content-Type '" + contentType + "' and type '" + typeToken + "'")))
                .doOnNext(v -> LOGGER.debug("Found a decoder for Content-Type='{}'", contentType));
    }

}
