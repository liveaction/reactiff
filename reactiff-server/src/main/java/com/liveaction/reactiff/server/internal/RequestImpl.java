package com.liveaction.reactiff.server.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.CodecManager;
import com.liveaction.reactiff.api.server.HttpMethod;
import com.liveaction.reactiff.api.server.Request;
import com.liveaction.reactiff.api.server.multipart.Part;
import com.liveaction.reactiff.api.server.route.Route;
import com.liveaction.reactiff.server.internal.multipart.FluxSinkMultipartListener;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.synchronoss.cloud.nio.multipart.Multipart;
import org.synchronoss.cloud.nio.multipart.MultipartContext;
import org.synchronoss.cloud.nio.multipart.MultipartUtils;
import org.synchronoss.cloud.nio.multipart.NioMultipartParser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

public final class RequestImpl implements Request {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestImpl.class);

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
    public Mono<Map<String, Part>> parts() {
        int contentLength = getContentLength(httpServerRequest.requestHeaders());
        String contentType = httpServerRequest.requestHeaders().get(HttpHeaderNames.CONTENT_TYPE);
        if (!MultipartUtils.isMultipart(contentType)) {
            return Mono.empty();
        }
        MultipartContext context = new MultipartContext(contentType,
                contentLength,
                httpServerRequest.requestHeaders().get(HttpHeaderNames.CONTENT_ENCODING));
        return Flux.<Part>create(sink -> {
            FluxSinkMultipartListener listener = new FluxSinkMultipartListener(sink, context);
            NioMultipartParser parser = Multipart.multipart(context).forNIO(listener);
            httpServerRequest.receive().asByteArray().subscribe(resultBytes -> {
                try {
                    parser.write(resultBytes);
                }
                catch (IOException ex) {
                    listener.onError("Exception thrown providing input to the parser", ex);
                }
            }, ex -> {
                try {
                    listener.onError("Request body input error", ex);
                    parser.close();
                }
                catch (IOException ex2) {
                    listener.onError("Exception thrown while closing the parser", ex2);
                }
            }, () -> {
                try {
                    parser.close();
                }
                catch (IOException ex) {
                    listener.onError("Exception thrown while closing the parser", ex);
                }
            });
        })
                .map(part -> Maps.immutableEntry(part.name(), part))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private int getContentLength(HttpHeaders headers) {
        // Until this is fixed https://github.com/synchronoss/nio-multipart/issues/10
        long length = Optional.ofNullable(headers.get(HttpHeaderNames.CONTENT_LENGTH))
                .map(Long::parseLong)
                .orElse(-1L);

        return (int) length == length ? (int) length : -1;
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
    public String query() {
        return new QueryStringDecoder(httpServerRequest.uri()).rawQuery();
    }

    @Override
    public String uri() {
        return new QueryStringDecoder(httpServerRequest.uri()).rawPath();
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

    @Override
    public Mono<Map<String, List<String>>> getFormData() {
        // Until this gets fixed in https://github.com/reactor/reactor-netty/pull/1411
        String contentType = httpServerRequest.requestHeaders().get(HttpHeaderNames.CONTENT_TYPE);
        if(!method().equals(HttpMethod.POST) || !contentType.toLowerCase().contains("application/x-www-form-urlencoded")) {
            return Mono.empty();
        }
        final Charset charset = HttpUtil.getCharset(contentType, CharsetUtil.UTF_8);
        return httpServerRequest.receive()
                .aggregate()
                .asByteArray()
                .map(bytes -> {
                    try {
                        return new QueryStringDecoder(new String(bytes, charset), false).parameters();
                    } catch (Exception e) {
                        throw e;
                    }
                });
    }

}
