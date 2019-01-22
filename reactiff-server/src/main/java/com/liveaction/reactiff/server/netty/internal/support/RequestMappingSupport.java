package com.liveaction.reactiff.server.netty.internal.support;

import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.codec.CodecManager;
import com.liveaction.reactiff.server.netty.FilterChain;
import com.liveaction.reactiff.server.netty.ReactiveFilter;
import com.liveaction.reactiff.server.netty.ReactiveHandler;
import com.liveaction.reactiff.server.netty.Request;
import com.liveaction.reactiff.server.netty.annotation.HttpMethod;
import com.liveaction.reactiff.server.netty.annotation.RequestMapping;
import com.liveaction.reactiff.server.netty.internal.RequestImpl;
import com.liveaction.reactiff.server.netty.internal.ResultUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.BiFunction;

public class RequestMappingSupport implements HandlerSupportFunction<RequestMapping> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestMappingSupport.class);

    private final CodecManager codecManager;
    private final Set<ReactiveFilter> reactiveFilters;

    public RequestMappingSupport(CodecManager codecManager, Set<ReactiveFilter> reactiveFilters) {
        this.codecManager = codecManager;
        this.reactiveFilters = reactiveFilters;
    }

    @Override
    public Class<RequestMapping> supports() {
        return RequestMapping.class;
    }

    @Override
    public int rank(RequestMapping annotation) {
        return annotation.rank();
    }

    @Override
    public void register(HttpServerRoutes httpServerRoutes, RequestMapping annotation, ReactiveHandler reactiveHandler, Method method) {
        FilterChain route = (req, res) -> {
            try {
                TypeToken<?> returnType = TypeToken.of(method.getGenericReturnType());
                Request request = new RequestImpl(req, codecManager);
                Object rawResult = method.invoke(reactiveHandler, request);
                return ResultUtils.toResult(returnType, rawResult);
            } catch (IllegalAccessException | InvocationTargetException error) {
                return Mono.error(error);
            }
        };
        BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> onRequest = (req, res) -> applyFilters(req, res, route);
        for (HttpMethod httpMethod : annotation.method()) {
            httpMethod.route(httpServerRoutes, annotation.path(), onRequest);
        }
        LOGGER.info("Registered route {} : '{}' -> {}", annotation.method(), annotation.path(), method);
    }

    private Publisher<Void> applyFilters(HttpServerRequest req, HttpServerResponse res, FilterChain route) {
        FilterChain filterChain = route;
        for (ReactiveFilter element : this.reactiveFilters) {
            filterChain = chain(element, filterChain);
        }
        return filterChain.chain(req, res)
                .flatMap(filteredResult -> {
                    NettyOutbound send = res.status(filteredResult.status()).send(codecManager.encode(req.requestHeaders(), filteredResult.data()));
                    return Mono.from(send);
                });
    }

    private FilterChain chain(ReactiveFilter element, FilterChain filterChain) {
        return (httpServerRequest, httpServerResponse) -> element.filter(httpServerRequest, httpServerResponse, filterChain);
    }

}
