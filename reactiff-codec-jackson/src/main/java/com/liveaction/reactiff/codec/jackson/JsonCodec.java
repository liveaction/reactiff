package com.liveaction.reactiff.codec.jackson;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.Codec;
import com.liveaction.reactiff.api.server.Result;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class JsonCodec extends JacksonCodec implements Codec {

    private static final String DEFAULT_CHARSET = StandardCharsets.UTF_8.name();

    public static final String APPLICATION_STREAM_JSON = "application/stream+json";
    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_STREAM_JSON_WITH_CHARSET = contentTypeWithCharset(APPLICATION_STREAM_JSON);
    public static final String APPLICATION_JSON_WITH_CHARSET = contentTypeWithCharset(APPLICATION_JSON);

    public static final ImmutableSet<String> KNOWN_CONTENT_TYPES = ImmutableSet.of(
            APPLICATION_STREAM_JSON,
            APPLICATION_JSON,
            APPLICATION_STREAM_JSON_WITH_CHARSET,
            APPLICATION_JSON_WITH_CHARSET);


    public JsonCodec(ObjectMapper objectCodec) {
        super(objectCodec, new JsonFactory());
    }

    @Override
    public int rank() {
        return 5;
    }

    @Override
    public boolean supports(String contentType, TypeToken<?> typeToken) {
        return KNOWN_CONTENT_TYPES.stream().anyMatch(contentType::contains);
    }

    @Override
    public <T> Mono<T> decodeMono(String contentType, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        return super.decodeMono(byteBufFlux, typeToken);
    }

    @Override
    public <T> Flux<T> decodeFlux(String contentType, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        return checkContentType(contentType)
                .map(Flux::<T>error)
                .orElseGet(() -> super.decodeFlux(byteBufFlux, typeToken, isApplicationJson(contentType)));
    }

    @Override
    public <T> Publisher<ByteBuf> encode(String contentType, Publisher<T> data, TypeToken<T> typeToken) {
        return checkContentType(contentType)
                .map(throwable -> (Publisher<ByteBuf>) Mono.<ByteBuf>error(throwable))
                .orElseGet(() -> super.encode(data, isApplicationJson(contentType)));
    }

    @Override
    public <T> Mono<Result<T>> enrich(Result<T> result, String contentType) {
        Result.Builder<T> resultBuilder = result.copy()
                .header(HttpHeaderNames.CONTENT_TYPE, contentTypeWithCharset(contentType), false);

        return Mono.just(resultBuilder.build());
    }

    private boolean isApplicationJson(String contentType) {
        return APPLICATION_JSON.equalsIgnoreCase(contentType) || APPLICATION_JSON_WITH_CHARSET.equalsIgnoreCase(contentType);
    }

    private static String contentTypeWithCharset(String contentType) {
        return contentType.contains("charset") ?
                contentType :
                contentType + "; charset=" + DEFAULT_CHARSET;
    }

    private Optional<Throwable> checkContentType(String contentType) {
        boolean unknownContentType = KNOWN_CONTENT_TYPES.stream()
                .noneMatch(knownTypes -> knownTypes.equalsIgnoreCase(contentType));

        if (unknownContentType) {
            return Optional.of(new IllegalArgumentException("Unsupported ContentType '" + contentType + "'"));
        }

        return Optional.empty();
    }

}
