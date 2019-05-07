package com.liveaction.reactiff.server.general.example;

import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.server.HttpMethod;
import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.api.server.Request;
import com.liveaction.reactiff.api.server.Result;
import com.liveaction.reactiff.api.server.annotation.RequestMapping;
import com.liveaction.reactiff.api.server.annotation.UriParam;
import com.liveaction.reactiff.api.server.annotation.WsMapping;
import com.liveaction.reactiff.server.mock.Pojo;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;

public final class TestController implements ReactiveHandler {

    @RequestMapping(method = HttpMethod.GET, path = "/failed")
    public Mono<Result<String>> failed() {
        throw new IllegalArgumentException("Always fail");
    }

    @RequestMapping(method = HttpMethod.GET, path = "/yes/nosuch")
    public Mono<Void> noSuchElementException(Request request) {
        return Mono.error(new NoSuchElementException("Element untel not found"));
    }

    @RequestMapping(method = HttpMethod.GET, path = "/yes/nosuchflux")
    public Mono<Result<Void>> noSuchElementExceptionFlux(Request request) {
        return Mono.error(new NoSuchElementException("Element untel not found"));
    }

    @RequestMapping(method = HttpMethod.GET, path = "/yes/unauthorized")
    public Mono<Void> unauthorizedException(Request request) {
        return Mono.error(new IllegalAccessException("Access forbidden by me"));
    }

    @RequestMapping(method = HttpMethod.GET, path = "/yes/{name}")
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

    @RequestMapping(method = HttpMethod.GET, path = "/boolean")
    public Mono<Boolean> getBoolean(Request request) {
        return Mono.just(true);
    }

    @RequestMapping(method = HttpMethod.GET, path = "/booleans")
    public Flux<Boolean> getBooleans(Request request) {
        return Flux.just(true, false);
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

    @RequestMapping(method = HttpMethod.POST, path = "/boolean/mono/from/flux")
    public Mono<Boolean> booleanMonoFromFlux(Request request) {
        return request.bodyToFlux(Boolean.class)
                .collectList()
                .map(ignored -> true);
    }

    @RequestMapping(method = HttpMethod.POST, path = "/boolean/flux/from/mono")
    public Flux<Boolean> booleanFluxFromMono(Request request) {
        return Flux.from(request.bodyToMono(Boolean.class)
                .map(ignored -> true));
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

    @RequestMapping(method = HttpMethod.POST, path = "/void")
    public void execute(Request request) {
        Boolean error = Boolean.valueOf(request.uriParam("error"));
        if (error) {
            throw new IllegalArgumentException("fail");
        }
    }

    @RequestMapping(method = HttpMethod.POST, path = "/monovoid")
    public Mono<Void> executeVoid() {
        return Mono.empty();
    }

    @RequestMapping(method = HttpMethod.GET, path = "/setCookie")
    public Mono<Result> setCookie() {
        Cookie c = new DefaultCookie("cookieName", "cookieValue");
        c.setHttpOnly(true);
        c.setSecure(true);
        c.setMaxAge(Duration.ofHours(1).getSeconds());
        return Mono.just(Result.builder()
                .status(HttpResponseStatus.OK.code(), "OK")
                .cookie(c)
                .build());
    }

}
