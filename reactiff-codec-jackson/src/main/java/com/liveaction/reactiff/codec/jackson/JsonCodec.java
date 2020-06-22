package com.liveaction.reactiff.codec.jackson;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.Codec;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class JsonCodec extends JacksonCodec implements Codec {

    public static final String APPLICATION_STREAM_JSON = "application/stream+json";
    public static final String APPLICATION_JSON = "application/json";


    public JsonCodec(ObjectMapper objectCodec) {
        super(objectCodec, new JsonFactory());
    }

    @Override
    public int rank() {
        return 5;
    }

    @Override
    public boolean supports(String contentType, TypeToken<?> typeToken) {
        return contentType.contains(APPLICATION_JSON) || contentType.equals(APPLICATION_STREAM_JSON);
    }

    @Override
    public <T> Mono<T> decodeMono(String contentType, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        return super.decodeMono(byteBufFlux, typeToken);
    }

    @Override
    public <T> Flux<T> decodeFlux(String contentType, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        if (APPLICATION_JSON.equalsIgnoreCase(contentType) || APPLICATION_STREAM_JSON.equalsIgnoreCase(contentType)) {
            return super.decodeFlux(byteBufFlux, typeToken, APPLICATION_JSON.equalsIgnoreCase(contentType));
        } else {
            return Flux.error(new IllegalArgumentException("Unsupported ContentType '" + contentType + "'"));
        }
    }

    @Override
    public <T> Publisher<ByteBuf> encode(String contentType, Publisher<T> data, TypeToken<T> typeToken) {
        if (APPLICATION_JSON.equalsIgnoreCase(contentType) || APPLICATION_STREAM_JSON.equalsIgnoreCase(contentType)) {
            return super.encode(data, APPLICATION_JSON.equalsIgnoreCase(contentType));
        } else {
            return Mono.error(new IllegalArgumentException("Unsupported ContentType '" + contentType + "'"));
        }
    }

}
