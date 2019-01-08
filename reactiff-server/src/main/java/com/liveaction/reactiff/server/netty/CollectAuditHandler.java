package com.liveaction.reactiff.server.netty;

import com.liveaction.reactiff.server.codec.CodecManager;
import com.liveaction.reactiff.server.netty.annotation.Get;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.util.NoSuchElementException;

public class CollectAuditHandler implements ReactiveHandler {

    private CodecManager codecManager;

    public void setCodecManager(CodecManager codecManager) {
        this.codecManager = codecManager;
    }

    @Get(path = "/yes/nosuch")
    public Publisher<Void> noSuchElementException(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
        return Mono.error(new NoSuchElementException("Element untel not found"));
    }

    @Get(path = "/yes/unauthorized")
    public Publisher<Void> unauthorizedException(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
        return Mono.error(new IllegalAccessException("Access forbidden by me"));
    }

    @Get(path = "/yes/{name}", rank = 1)
    public Publisher<Void> yes(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
        Publisher<ByteBuf> response = codecManager.encode(httpServerRequest.requestHeaders(), httpServerResponse, Flux.just("Hey " + httpServerRequest.param("name"), "Hey baby !"));
        return httpServerResponse.send(response);
    }

}
