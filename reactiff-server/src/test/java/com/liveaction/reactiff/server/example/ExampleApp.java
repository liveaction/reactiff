package com.liveaction.reactiff.server.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.liveaction.reactiff.api.codec.CodecManager;
import com.liveaction.reactiff.codec.CodecManagerImpl;
import com.liveaction.reactiff.codec.jackson.JsonCodec;
import com.liveaction.reactiff.server.DefaultFilters;
import com.liveaction.reactiff.server.ReactiveHttpServer;
import com.liveaction.reactiff.server.mock.Pojo;
import reactor.core.publisher.Flux;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;

public final class ExampleApp {

    public static void main(String[] args) {
        CodecManager codecManager = new CodecManagerImpl();
        codecManager.addCodec(new JsonCodec(new ObjectMapper()));

        try (ReactiveHttpServer server = ReactiveHttpServer.create()
                .protocols(HttpProtocol.HTTP11)
                .codecManager(codecManager)
                .port(3000)
                .build()) {

            server.addReactiveFilter(DefaultFilters.cors(
                    ImmutableSet.of("*"),
                    ImmutableSet.of("X-UserHeader"),
                    ImmutableSet.of("GET")
                    , true,
                    -1
            ));
            server.addReactiveHandler(new PojoHandler());
            server.start();

            Flux<Pojo> response = HttpClient.create()
                    .get()
                    .uri("http://localhost:3000/pojo")
                    .response(codecManager.decodeAsFlux(Pojo.class));

            response.subscribe(System.out::println);

            System.out.println(String.format("Received %d results", response.count().block()));
        }
    }

}
