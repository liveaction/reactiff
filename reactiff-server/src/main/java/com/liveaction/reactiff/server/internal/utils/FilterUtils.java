package com.liveaction.reactiff.server.internal.utils;

import com.liveaction.reactiff.api.codec.CodecManager;
import com.liveaction.reactiff.api.server.FilterChain;
import com.liveaction.reactiff.api.server.ReactiveFilter;
import com.liveaction.reactiff.api.server.Request;
import com.liveaction.reactiff.api.server.Result;
import com.liveaction.reactiff.api.server.route.Route;
import com.liveaction.reactiff.server.internal.RequestImpl;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

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
        Mono<Result<?>> enrichedResult = filterChain.chain(request)
                .onErrorResume(throwable -> {
                    int status = 500;
                    LOGGER.error("Unexpected error for {}", req.uri(), throwable);
                    String message = throwable.getMessage();
                    if (message == null) {
                        message = HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase();
                    }
                    if (writeErrorStacktrace) {
                        return Mono.just(Result.<Throwable>builder()
                                .status(status, message)
                                .data(Mono.just(throwable), Throwable.class)
                                .build());
                    } else {
                        return Mono.just(Result.withStatus(status, message));
                    }
                })
                .flatMap(result -> (Mono<Result<?>>) codecManager.enrichResult(req.requestHeaders(), res.responseHeaders(), result));
        return enrichedResult
                .flatMap(result -> {
                    if (res.isWebsocket()) {
                        return Mono.empty();
                    }
                    HttpServerResponse httpServerResponse = res.status(result.status());
                    result.headers().forEach(res::addHeader);
                    result.cookies().forEach(res::addCookie);
                    Publisher<?> data = result.data();
                    if (data == null) {
                        return Mono.from(httpServerResponse.send());
                    } else {
                        return Mono.from(httpServerResponse
                                .send(encodeResult(req, res, codecManager, result)));
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
