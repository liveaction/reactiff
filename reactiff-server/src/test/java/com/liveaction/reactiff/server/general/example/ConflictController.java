package com.liveaction.reactiff.server.general.example;

import com.liveaction.reactiff.api.server.HttpMethod;
import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.api.server.Request;
import com.liveaction.reactiff.api.server.annotation.RequestMapping;
import com.liveaction.reactiff.api.server.annotation.WsMapping;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

public final class ConflictController implements ReactiveHandler {

    @WsMapping(path = "/conflict/ws", rank = -1)
    public Publisher<Void> yesWebSocket(WebsocketInbound in, WebsocketOutbound out) {
        return out.sendString(Flux.just("Salut !", "Je m'appelle", "Jean Baptiste Poquelin"))
                .then(out.sendClose());
    }

    @RequestMapping(method = HttpMethod.GET, path = "/conflict/{name}")
    public Flux<String> abc(Request request) {
        return Flux.just("Hey " + request.pathParam("name"), "Hey baby !");
    }

    @RequestMapping(method = HttpMethod.GET, path = "/conflict/test")
    public Flux<String> conflixtTest(Request request) {
        return Flux.just("This is a test !");
    }


}
