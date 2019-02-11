package com.liveaction.reactiff.server.internal;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.liveaction.reactiff.api.codec.CodecManager;
import com.liveaction.reactiff.api.server.FilterChain;
import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.api.server.Request;
import com.liveaction.reactiff.api.server.Result;
import com.liveaction.reactiff.server.internal.support.HandlerSupportFunction;
import com.liveaction.reactiff.server.internal.support.RequestMappingSupport;
import com.liveaction.reactiff.server.internal.support.WsMappingSupport;
import org.reactivestreams.Publisher;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

public class Router implements BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> {

    private final ImmutableSet<HandlerSupportFunction<? extends Annotation>> handlerSupportFunctions;

    private final Set<ReactiveHandler> reactiveHandlers = Collections.synchronizedSortedSet(new TreeSet<>());

    private final CodecManager codecManager;
    private final Function<FilterChain, FilterChain> filterFunction;
    private HttpServerRoutes httpServerRoutes = HttpServerRoutes.newRoutes();

    public Router(CodecManager codecManager, Function<FilterChain, FilterChain> filterFunction) {
        this.codecManager = codecManager;
        this.filterFunction = filterFunction;
        this.handlerSupportFunctions = ImmutableSet.of(
                new RequestMappingSupport(codecManager, filterFunction),
                new WsMappingSupport()
        );
    }

    public void addReactiveHander(ReactiveHandler reactiveHandler) {
        this.reactiveHandlers.add(reactiveHandler);
        logRegister(reactiveHandler);
        updateRoutes();
    }

    public void removeReactiveHander(ReactiveHandler reactiveHandler) {
        this.reactiveHandlers.remove(reactiveHandler);
        logUnregister(reactiveHandler);
        updateRoutes();
    }

    @Override
    public Publisher<Void> apply(HttpServerRequest request, HttpServerResponse response) {
        return httpServerRoutes.apply(request, response);
    }

    private void updateRoutes() {
        HttpServerRoutes httpServerRoutes = HttpServerRoutes.newRoutes();
        handlerSupportFunctions.forEach(handlerSupportFunction -> reactiveHandlers.forEach(rh -> registerMethod(httpServerRoutes, rh, handlerSupportFunction)));
        httpServerRoutes.route(httpServerRequest -> true, (req, res) -> FilterUtils.applyFilters(req, res, codecManager, filterFunction, this::notFound, Optional.empty()));
        this.httpServerRoutes = httpServerRoutes;
    }

    private void logRegister(ReactiveHandler reactiveHandler) {
        handlerSupportFunctions.forEach(handlerSupportFunction -> getAnnotatedMethods(reactiveHandler, handlerSupportFunction)
                .forEach(methodEntry ->
                        LoggerFactory.getLogger(Router.class).info("Register route {} : {}", methodEntry.getKey(), methodEntry.getValue())
                ));
    }

    private void logUnregister(ReactiveHandler reactiveHandler) {
        handlerSupportFunctions.forEach(handlerSupportFunction -> getAnnotatedMethods(reactiveHandler, handlerSupportFunction)
                .forEach(methodEntry ->
                        LoggerFactory.getLogger(Router.class).info("Unregister route {} : {}", methodEntry.getKey(), methodEntry.getValue())
                ));
    }

    private <T extends Annotation> void registerMethod(HttpServerRoutes httpServerRoutes, ReactiveHandler reactiveHandler, HandlerSupportFunction<T> handlerSupportFunction) {
        getAnnotatedMethods(reactiveHandler, handlerSupportFunction)
                .forEach(e -> handlerSupportFunction.register(httpServerRoutes, e.getKey(), reactiveHandler, e.getValue()));
    }

    private <T extends Annotation> Stream<Map.Entry<T, Method>> getAnnotatedMethods(ReactiveHandler reactiveHandler, HandlerSupportFunction<T> handlerSupportFunction) {
        return Stream.of(reactiveHandler.getClass().getDeclaredMethods())
                .map(m -> Maps.immutableEntry(m.getAnnotation(handlerSupportFunction.supports()), m))
                .filter(e -> e.getKey() != null)
                .sorted(Comparator.comparingInt(o -> handlerSupportFunction.rank(o.getKey())));
    }

    private Mono<Result> notFound(Request request) {
        return Mono.just(Result.withStatus(404, String.format("'%s' not found", request.uri())));
    }

}