package com.liveaction.reactiff.codec.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.codec.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.netty.ByteBufFlux;

import java.lang.reflect.Type;

public final class JsonCodec implements Codec {

    public static final String APPLICATION_STREAM_JSON = "application/stream+json";
    public static final String APPLICATION_JSON = "application/json";

    private ObjectMapper objectMapper;

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public int rank() {
        return 1;
    }

    @Override
    public boolean supports(String contentType) {
        return APPLICATION_JSON.equals(contentType) || APPLICATION_STREAM_JSON.equals(contentType);
    }

    @Override
    public <T> Publisher<T> decode(String contentType, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        return ByteBufFlux.fromInbound(byteBufFlux)
                .aggregate()
                .asInputStream()
//                .map(inputStream -> objectMapper.readValue(inputStream, typeToken)); //TODO TypeReference ?
                .map(inputStream -> null);
    }

    @Override
    public <T> Publisher<ByteBuf> encode(String contentType, Publisher<T> data) {
        return Flux.from(data)
                .map(t -> {
                    try {
                        return Unpooled.wrappedBuffer(objectMapper.writeValueAsBytes(t));
                    } catch (JsonProcessingException e) {
                        throw new IllegalStateException("Unable to encode as JSON", e);
                    }
                });
    }

    private <T> TypeReference<T> toTypeReference(TypeToken<T> typeToken) {
        return new TypeReference<T>() {
            @Override
            public Type getType() {
                return typeToken.getType();
            }
        };
    }

}
