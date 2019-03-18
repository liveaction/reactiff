package com.liveaction.reactiff.server.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liveaction.reactiff.api.codec.CodecManager;
import com.liveaction.reactiff.codec.CodecManagerImpl;
import com.liveaction.reactiff.codec.jackson.JsonCodec;
import com.liveaction.reactiff.server.ReactiveHttpServer;
import reactor.netty.http.HttpProtocol;

public final class ExampleApp {

    public static void main(String[] args) {
        CodecManager codecManager = new CodecManagerImpl();
        codecManager.addCodec(new JsonCodec(new ObjectMapper()));

        ReactiveHttpServer server = ReactiveHttpServer.create()
                .protocols(HttpProtocol.HTTP11)
                .codecManager(codecManager)
                .handler(new PojoHandler())
                .port(3000)
                .build();

        server.startAndWait();
    }

}
