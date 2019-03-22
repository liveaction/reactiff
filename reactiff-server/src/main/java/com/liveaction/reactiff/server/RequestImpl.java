package com.liveaction.reactiff.server;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.CodecManager;
import com.liveaction.reactiff.api.server.HttpMethod;
import com.liveaction.reactiff.api.server.Request;
import com.liveaction.reactiff.api.server.route.Route;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class RequestImpl implements Request {

    private final HttpServerRequest httpServerRequest;
    private final CodecManager codecManager;
    private final ImmutableMap<String, ImmutableList<String>> parameters;
    private final HttpMethod httpMethod;
    private final Route matchingRoute;

    public RequestImpl(HttpServerRequest httpServerRequest, CodecManager codecManager, Optional<Route> matchingRoute) {
        this.httpServerRequest = httpServerRequest;
        this.codecManager = codecManager;
        QueryStringDecoder decoder = new QueryStringDecoder(httpServerRequest.uri());
        parameters = ImmutableMap.copyOf(Maps.transformValues(decoder.parameters(), ImmutableList::copyOf));
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
        List<String> values = parameters.get(name);
        if (values != null) {
            return ImmutableList.copyOf(values);
        } else {
            return ImmutableList.of();
        }
    }

    @Override
    public ImmutableMap<String, ImmutableList<String>> uriParams() {
        return parameters;
    }

    @Override
    public String pathParam(String name) {
        return QueryStringDecoder.decodeComponent(httpServerRequest.param(name));
    }

    @Override
    public String header(CharSequence name) {
        return httpServerRequest.requestHeaders().get(name);
    }

    @Override
    public List<String> headers(CharSequence name) {
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

    @Override
    public Locale getLocale() {
        ImmutableList<Locale.LanguageRange> languageRanges = getLanguageRanges();
        List<Locale> locales = Locale.filter(languageRanges, ImmutableList.copyOf(Locale.getAvailableLocales()));
        if (locales.isEmpty()) {
            return Locale.getDefault();
        } else {
            return locales.get(0);
        }
    }

    @Override
    public ImmutableList<Locale.LanguageRange> getLanguageRanges() {
        return Optional.ofNullable(header(HttpHeaderNames.ACCEPT_LANGUAGE))
                .map(Locale.LanguageRange::parse)
                .map(ImmutableList::copyOf)
                .orElseGet(ImmutableList::of);
    }

}
