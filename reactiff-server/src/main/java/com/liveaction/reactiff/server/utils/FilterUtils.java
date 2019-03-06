package com.liveaction.reactiff.server.utils;

import com.liveaction.reactiff.api.codec.CodecManager;
import com.liveaction.reactiff.api.server.FilterChain;
import com.liveaction.reactiff.api.server.ReactiveFilter;
import com.liveaction.reactiff.api.server.Request;
import com.liveaction.reactiff.api.server.Result;
import com.liveaction.reactiff.api.server.route.Route;
import com.liveaction.reactiff.server.RequestImpl;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.NettyPipeline;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.function.Function;

public final class FilterUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterUtils.class);

    private FilterUtils() {
    }

    @SuppressWarnings("unchecked")
    public static Publisher<Void> applyFilters(HttpServerRequest req,
                                               HttpServerResponse res,
                                               CodecManager codecManager,
                                               Function<FilterChain, FilterChain> chainFunction,
                                               FilterChain chain,
                                               Optional<Route> matchingRoute,
                                               boolean writeErrorStacktrace) {
        Request request = new RequestImpl(req, codecManager, matchingRoute);
        FilterChain filterChain = chainFunction.apply(chain);
        return filterChain.chain(request)
                .onErrorResume(throwable -> {
                    int status = 500;
                    LOGGER.error("Unexpected error", throwable);
                    if (writeErrorStacktrace) {
                        StringWriter stringWriter = new StringWriter();
                        throwable.printStackTrace(new PrintWriter(stringWriter));
                        return Mono.just(Result.withStatus(status, throwable.getMessage(), stringWriter.toString()));
                    } else {
                        return Mono.just(Result.withStatus(status, throwable.getMessage()));
                    }
                })
                .flatMap(filteredResult -> {
                    filteredResult.headers().forEach(res::header);
                    HttpServerResponse httpServerResponse = res.status(filteredResult.status());
                    Publisher<?> data = filteredResult.data();
                    if (data == null) {
                        return Mono.from(httpServerResponse.send());
                    } else {
                        return Mono.from(httpServerResponse
                                .options(NettyPipeline.SendOptions::flushOnEach)
                                .send(encodeResult(req, res, codecManager, filteredResult)));
                    }
                });
    }

    private static <T> Publisher<ByteBuf> encodeResult(HttpServerRequest req, HttpServerResponse res, CodecManager codecManager, Result<T> filteredResult) {
        return codecManager.encode(req.requestHeaders(), res.responseHeaders(), filteredResult.data(), filteredResult.type());
    }

    public static FilterChain chain(ReactiveFilter element, FilterChain filterChain) {
        return (request) -> element.filter(request, filterChain);
    }

}
