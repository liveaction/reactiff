package com.liveaction.reactiff.server.netty.internal;

import com.liveaction.reactiff.codec.CodecManager;
import com.liveaction.reactiff.server.netty.FilterChain;
import com.liveaction.reactiff.server.netty.ReactiveFilter;
import com.liveaction.reactiff.server.netty.Request;
import com.liveaction.reactiff.server.netty.Route;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.NettyPipeline;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.util.Optional;
import java.util.function.Function;

public final class FilterUtils {

    private FilterUtils() {

    }

    public static Publisher<Void> applyFilters(HttpServerRequest req, HttpServerResponse res, CodecManager codecManager, Function<FilterChain, FilterChain> chainFunction, FilterChain chain, Optional<Route> matchingRoute) {
        Request request = new RequestImpl(req, codecManager, matchingRoute);
        FilterChain filterChain = chainFunction.apply(chain);
        return filterChain.chain(request)
                .flatMap(filteredResult -> {
                    filteredResult.headers().forEach(res::header);
                    HttpServerResponse httpServerResponse = res.status(filteredResult.status());
                    Publisher<?> data = filteredResult.data();
                    if (data == null) {
                        return Mono.from(httpServerResponse.send());
                    } else {
                        return Mono.from(httpServerResponse
                                .options(NettyPipeline.SendOptions::flushOnEach)
                                .send(codecManager.encode(req.requestHeaders(), res.responseHeaders(), data)));
                    }
                });
    }

    static FilterChain chain(ReactiveFilter element, FilterChain filterChain) {
        return (request) -> element.filter(request, filterChain);
    }

}
