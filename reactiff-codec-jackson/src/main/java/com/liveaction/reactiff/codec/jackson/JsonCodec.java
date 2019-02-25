package com.liveaction.reactiff.codec.jackson;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.ObjectCodec;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.Codec;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class JsonCodec implements Codec {

    public static final String APPLICATION_STREAM_JSON = "application/stream+json";
    public static final String APPLICATION_JSON = "application/json";

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonCodec.class);

    private final JacksonCodec jacksonCodec;

    public JsonCodec(ObjectCodec objectCodec) {
        this.jacksonCodec = new JacksonCodec(objectCodec, new JsonFactory(), new byte[]{'\n'});
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
        return jacksonCodec.decodeMono(byteBufFlux, typeToken);
    }

    @Override
    public <T> Flux<T> decodeFlux(String contentType, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        return jacksonCodec.decodeFlux(byteBufFlux, typeToken);
    }

    @Override
    public <T> Publisher<ByteBuf> encode(String contentType, Publisher<T> data, TypeToken<T> typeToken) {
        if (APPLICATION_JSON.equalsIgnoreCase(contentType) || APPLICATION_STREAM_JSON.equalsIgnoreCase(contentType)) {
            return jacksonCodec.encode(data, APPLICATION_JSON.equalsIgnoreCase(contentType));
        } else {
            throw new IllegalArgumentException("Unsupported ContentType '" + contentType + "'");
        }
    }

}
