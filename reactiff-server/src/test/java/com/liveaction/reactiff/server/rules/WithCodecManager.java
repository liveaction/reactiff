package com.liveaction.reactiff.server.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.CodecManager;
import com.liveaction.reactiff.codec.CodecManagerImpl;
import com.liveaction.reactiff.codec.RawBinaryCodec;
import com.liveaction.reactiff.codec.RawFileCodec;
import com.liveaction.reactiff.codec.TextPlainCodec;
import com.liveaction.reactiff.codec.jackson.JsonCodec;
import com.liveaction.reactiff.codec.jackson.SmileBinaryCodec;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.rules.ExternalResource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClientResponse;

import java.util.function.BiFunction;

public final class WithCodecManager extends ExternalResource {

    public CodecManager codecManager;

    public WithCodecManager() {
        this(new ObjectMapper());
    }

    public WithCodecManager(ObjectMapper objectMapper) {
        codecManager = new CodecManagerImpl();
        codecManager.addCodec(new JsonCodec(objectMapper));
        codecManager.addCodec(new SmileBinaryCodec(objectMapper));
        codecManager.addCodec(new TextPlainCodec());
        codecManager.addCodec(new RawBinaryCodec());
        codecManager.addCodec(new RawFileCodec());
    }

    public <T> BiFunction<HttpClientResponse, ByteBufFlux, Mono<T>> checkErrorAndDecodeAsMono(Class<T> clazz) {
        return checkErrorAndDecodeAsMono(TypeToken.of(clazz));
    }

    public <T> BiFunction<HttpClientResponse, ByteBufFlux, Mono<T>> checkErrorAndDecodeAsMono(TypeToken<T> typeToken) {
        return (response, flux) -> {
            HttpResponseStatus status = response.status();
            if (status.code() == 200) {
                return codecManager.decodeAsMono(typeToken).apply(response, flux);
            } else {
                return Mono.error(new HttpException(status.code(), status.code() + " : " + status.reasonPhrase()));
            }
        };
    }

    public <T> BiFunction<HttpClientResponse, ByteBufFlux, Flux<T>> checkErrorAndDecodeAsFlux(Class<T> clazz) {
        return (response, flux) -> {
            HttpResponseStatus status = response.status();
            if (status.code() == 200) {
                return codecManager.decodeAsFlux(clazz).apply(response, flux);
            } else {
                return Flux.error(new HttpException(status.code(), status.code() + " : " + status.reasonPhrase()));
            }
        };
    }

}
