package com.liveaction.reactiff.server.example;

import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.server.HttpMethod;
import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.api.server.Request;
import com.liveaction.reactiff.api.server.annotation.RequestMapping;
import com.liveaction.reactiff.api.server.annotation.WsMapping;
import com.liveaction.reactiff.server.example.api.Pojo;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

import java.util.NoSuchElementException;

public class TestController implements ReactiveHandler {

    @RequestMapping(method = HttpMethod.GET, path = "/yes/nosuch")
    public Mono<Void> noSuchElementException(Request request) {
        return Mono.error(new NoSuchElementException("Element untel not found"));
    }

    @RequestMapping(method = HttpMethod.GET, path = "/yes/unauthorized")
    public Mono<Void> unauthorizedException(Request request) {
        return Mono.error(new IllegalAccessException("Access forbidden by me"));
    }

    @RequestMapping(method = HttpMethod.GET, path = "/yes/{name}", rank = 1)
    public Flux<String> yes(Request request) {
        return Flux.just("Hey " + request.pathParam("name"), "Hey baby !");
    }

    @WsMapping(path = "/websocket")
    public Publisher<Void> yesWebSocket(WebsocketInbound in, WebsocketOutbound out) {
        return out.sendString(Flux.just("Salut !", "Je m'appelle", "Jean Baptiste Poquelin"))
                .then(out.sendClose());
    }

    @RequestMapping(method = HttpMethod.POST, path = "/yes")
    public Flux<Pojo> postPojo(Request request) {
        return request.bodyToFlux(new TypeToken<Pojo>() {
        })
                .map(pojo -> new Pojo(pojo.id, pojo.name + " from server"));
    }

    @RequestMapping(method = HttpMethod.POST, path = "/yes/heavy")
    public Flux<Pojo> postHeavyPojo(Request request) {
        int count = Integer.valueOf(request.uriParam("count"));
        return Flux.range(0, count)
                .map(i -> new Pojo("id", "value_" + i));
    }

    @RequiresAuth(authorized = true)
    @RequestMapping(method = HttpMethod.GET, path = "/oui")
    public Mono<String> authorized() {
        return Mono.just("oui");
    }

    @RequiresAuth(authorized = false)
    @RequestMapping(method = HttpMethod.GET, path = "/non")
    public Mono<String> unauthorized() {
        return Mono.just("non");
    }

    @RequestMapping(method = HttpMethod.POST, path = "/upload")
    public Flux<byte[]> upload(Request request) {
        return request.bodyToFlux(new TypeToken<byte[]>() {
        });
    }

}
