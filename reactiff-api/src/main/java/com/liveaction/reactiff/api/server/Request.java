package com.liveaction.reactiff.api.server;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.Cookie;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface Request {

    <T> Mono<T> bodyToMono(TypeToken<T> typeToken);

    <T> Flux<T> bodyToFlux(TypeToken<T> typeToken);

    String uriParam(String name);

    ImmutableList<String> uriParams(String name);

    String pathParam(String name);

    String header(String name);

    /**
     * The list of values for this header, empty if no header found with this name.
     */
    List<String> headers(String name);

    // delegates methods

    InetSocketAddress hostAddress();

    InetSocketAddress remoteAddress();

    HttpHeaders requestHeaders();

    String scheme();

    Map<CharSequence, Set<Cookie>> cookies();

    boolean isKeepAlive();

    boolean isWebsocket();

    HttpMethod method();

    String path();

    String uri();

    HttpVersion version();

    Optional<Route> matchingRoute();

}
