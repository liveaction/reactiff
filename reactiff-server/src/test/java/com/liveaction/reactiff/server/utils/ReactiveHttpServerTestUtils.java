package com.liveaction.reactiff.server.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.liveaction.reactiff.api.codec.CodecManager;
import com.liveaction.reactiff.codec.CodecManagerImpl;
import com.liveaction.reactiff.codec.RawBinaryCodec;
import com.liveaction.reactiff.codec.RawFileCodec;
import com.liveaction.reactiff.codec.TextPlainCodec;
import com.liveaction.reactiff.codec.jackson.JsonCodec;
import com.liveaction.reactiff.codec.jackson.SmileBinaryCodec;
import com.liveaction.reactiff.server.HttpException;
import com.liveaction.reactiff.server.ReactiveHttpServer;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.commons.io.IOUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.BiFunction;

public class ReactiveHttpServerTestUtils {
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static CodecManager codecManager;

    static {
        codecManager = new CodecManagerImpl();
        codecManager.addCodec(new JsonCodec(objectMapper));
        codecManager.addCodec(new SmileBinaryCodec(objectMapper));
        codecManager.addCodec(new TextPlainCodec());
        codecManager.addCodec(new RawBinaryCodec());
        codecManager.addCodec(new RawFileCodec());
    }

    public static <T> BiFunction<HttpClientResponse, ByteBufFlux, Mono<T>> checkErrorAndDecodeAsMono(Class<T> clazz) {
        return (response, flux) -> {
            HttpResponseStatus status = response.status();
            if (status.code() == 200) {
                return codecManager.decodeAsMono(clazz).apply(response, flux);
            } else {
                return Mono.error(new HttpException(status.code(), status.code() + " : " + status.reasonPhrase()));
            }
        };
    }

    public static <T> BiFunction<HttpClientResponse, ByteBufFlux, Flux<T>> checkErrorAndDecodeAsFlux(Class<T> clazz) {
        return (response, flux) -> {
            HttpResponseStatus status = response.status();
            if (status.code() == 200) {
                return codecManager.decodeAsFlux(clazz).apply(response, flux);
            } else {
                return Flux.error(new HttpException(status.code(), status.code() + " : " + status.reasonPhrase()));
            }
        };
    }

    public static HttpClient httpClient(ReactiveHttpServer tested) {
        return HttpClient.create()
                .protocol(HttpProtocol.HTTP11)
                .baseUrl("http://localhost:" + tested.port())
                .headers(httpHeaders -> httpHeaders.set(HttpHeaderNames.ACCEPT, "application/json"));
    }

    public static Mono<byte[]> asBinary(Flux<byte[]> data) {
        return ByteBufFlux.fromInbound(data).aggregate().asInputStream()
                .map(inputStream -> {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    try {
                        IOUtils.copy(inputStream, out);
                    } catch (IOException e) {
                        throw Throwables.propagate(e);
                    }
                    return out.toByteArray();
                });
    }

    public static Mono<String> asString(Flux<byte[]> data) {
        return asBinary(data).map(b -> new String(b, Charsets.UTF_8));
    }
}
