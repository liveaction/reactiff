package com.liveaction.reactiff.server.netty.annotation;

import org.reactivestreams.Publisher;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;

import java.util.function.BiFunction;

public enum HttpMethod {
    OPTIONS(HttpServerRoutes::options),
    HEAD(HttpServerRoutes::head),
    GET(HttpServerRoutes::get),
    POST(HttpServerRoutes::post),
    PUT(HttpServerRoutes::put),
    DELETE(HttpServerRoutes::delete);

    private final RoutingFunction routingFunction;

    @FunctionalInterface
    interface RoutingFunction {
        HttpServerRoutes route(HttpServerRoutes httpServerRoutes, String path, BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler);
    }

    HttpMethod(RoutingFunction routingFunction) {
        this.routingFunction = routingFunction;
    }

    public HttpServerRoutes route(HttpServerRoutes httpServerRoutes, String path, BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler) {
        return routingFunction.route(httpServerRoutes, path, handler);
    }

}
