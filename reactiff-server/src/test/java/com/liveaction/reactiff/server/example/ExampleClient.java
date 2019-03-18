package com.liveaction.reactiff.server.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liveaction.reactiff.api.codec.CodecManager;
import com.liveaction.reactiff.codec.CodecManagerImpl;
import com.liveaction.reactiff.codec.jackson.JsonCodec;
import com.liveaction.reactiff.server.mock.Pojo;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

public final class ExampleClient {

    public static void main(String[] args) {
        CodecManager codecManager = new CodecManagerImpl();
        codecManager.addCodec(new JsonCodec(new ObjectMapper()));

        Flux<Pojo> response = HttpClient.create()
                .get()
                .uri("http://localhost:3000/pojo")
                .response(codecManager.decodeAsFlux(Pojo.class));

        response.subscribe(System.out::println);

        System.out.println(String.format("Received %d results", response.count().block()));
    }

}
