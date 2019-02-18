package com.liveaction.reactiff.server.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.CodecManager;
import com.liveaction.reactiff.api.server.HttpMethod;
import com.liveaction.reactiff.api.server.Request;
import com.liveaction.reactiff.api.server.Route;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class RequestImpl implements Request {

    private final HttpServerRequest httpServerRequest;
    private final CodecManager codecManager;
    private final ImmutableMap<String, List<String>> parameters;
    private final HttpMethod httpMethod;
    private final Route matchingRoute;

    public RequestImpl(HttpServerRequest httpServerRequest, CodecManager codecManager, Optional<Route> matchingRoute) {
        this.httpServerRequest = httpServerRequest;
        this.codecManager = codecManager;
        QueryStringDecoder decoder = new QueryStringDecoder(httpServerRequest.uri());
        parameters = ImmutableMap.copyOf(decoder.parameters());
        httpMethod = HttpMethod.valueOf(httpServerRequest.method().name());
        this.matchingRoute = matchingRoute.orElse(null);
    }

    @Override
    public <T> Mono<T> bodyToMono(TypeToken<T> typeToken) {
        return codecManager.decodeAsMono(httpServerRequest, typeToken);
    }

    @Override
    public <T> Flux<T> bodyToFlux(TypeToken<T> typeToken) {
        return codecManager.decodeAsFlux(httpServerRequest, typeToken);
    }

    @Override
    public String uriParam(String name) {
        List<String> values = parameters.get(name);
        if (values != null && !values.isEmpty()) {
            return values.get(0);
        } else {
            return null;
        }
    }

    @Override
    public ImmutableList<String> uriParams(String name) {
        return ImmutableList.copyOf(parameters.get(name));
    }

    @Override
    public ImmutableMap<String, List<String>> uriParams() {
        return parameters;
    }

    @Override
    public String pathParam(String name) {
        return httpServerRequest.param(name);
    }

    @Override
    public String header(String name) {
        return httpServerRequest.requestHeaders().get(name);
    }

    @Override
    public List<String> headers(String name) {
        return httpServerRequest.requestHeaders().getAll(name);
    }

    @Override
    public InetSocketAddress hostAddress() {
        return httpServerRequest.hostAddress();
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return httpServerRequest.remoteAddress();
    }

    @Override
    public HttpHeaders requestHeaders() {
        return httpServerRequest.requestHeaders();
    }

    @Override
    public String scheme() {
        return httpServerRequest.scheme();
    }

    @Override
    public Map<CharSequence, Set<Cookie>> cookies() {
        return httpServerRequest.cookies();
    }

    @Override
    public boolean isKeepAlive() {
        return httpServerRequest.isKeepAlive();
    }

    @Override
    public boolean isWebsocket() {
        return httpServerRequest.isWebsocket();
    }

    @Override
    public HttpMethod method() {
        return httpMethod;
    }

    @Override
    public String path() {
        return httpServerRequest.path();
    }

    @Override
    public String uri() {
        return httpServerRequest.uri();
    }

    @Override
    public HttpVersion version() {
        return httpServerRequest.version();
    }

    @Override
    public Optional<Route> matchingRoute() {
        return Optional.ofNullable(matchingRoute);
    }

}
