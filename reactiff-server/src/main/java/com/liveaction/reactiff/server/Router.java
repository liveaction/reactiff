package com.liveaction.reactiff.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.liveaction.reactiff.api.codec.CodecManager;
import com.liveaction.reactiff.api.server.FilterChain;
import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.api.server.Request;
import com.liveaction.reactiff.api.server.Result;
import com.liveaction.reactiff.api.server.route.Route;
import com.liveaction.reactiff.server.support.HandlerSupportFunction;
import com.liveaction.reactiff.server.support.RequestMappingSupport;
import com.liveaction.reactiff.server.support.WsMappingSupport;
import com.liveaction.reactiff.server.template.TemplateEngineImpl;
import com.liveaction.reactiff.server.utils.FilterUtils;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.reactivestreams.Publisher;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.liveaction.reactiff.api.server.utils.FormatUtils.formatRoutes;
import static java.util.stream.Collectors.toList;

public class Router implements BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> {

    private static final Comparator<Route> ROUTE_ORDER = Comparator.comparingInt(Route::rank)
            .thenComparing(Route::descriptor)
            .thenComparing(Route::path)
            .thenComparing(r -> r.handlerMethod().getName());

    private static final String NOT_FOUND_TEMPLATE = "/templates/not-found.txt";
    private static final TemplateEngineImpl TEMPLATE_ENGINE = new TemplateEngineImpl();

    private final ImmutableSet<HandlerSupportFunction<? extends Annotation, ? extends Route>> handlerSupportFunctions;

    private final Set<ReactiveHandler> reactiveHandlers = Collections.synchronizedSortedSet(new TreeSet<>());

    private final CodecManager codecManager;
    private final Function<FilterChain, FilterChain> filterFunction;
    private HttpServerRoutes httpServerRoutes = HttpServerRoutes.newRoutes();

    private final boolean writeErrorStacktrace;

    public Router(CodecManager codecManager, Function<FilterChain, FilterChain> filterFunction, boolean writeErrorStacktrace) {
        this.codecManager = codecManager;
        this.filterFunction = filterFunction;
        this.handlerSupportFunctions = ImmutableSet.of(
                new RequestMappingSupport(codecManager, filterFunction, writeErrorStacktrace),
                new WsMappingSupport()
        );
        this.writeErrorStacktrace = writeErrorStacktrace;
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
        httpServerRoutes.route(httpServerRequest -> true, (req, res) -> FilterUtils.applyFilters(req, res, codecManager, filterFunction, this::notFound, Optional.empty(), writeErrorStacktrace));
        this.httpServerRoutes = httpServerRoutes;
    }

    private void logRegister(ReactiveHandler reactiveHandler) {
        handlerSupportFunctions.forEach(handlerSupportFunction -> getAnnotatedRoutes(reactiveHandler, handlerSupportFunction)
                .forEach(route ->
                        LoggerFactory.getLogger(Router.class).info("Register route {}", route)
                ));
    }

    private void logUnregister(ReactiveHandler reactiveHandler) {
        handlerSupportFunctions.forEach(handlerSupportFunction -> getAnnotatedRoutes(reactiveHandler, handlerSupportFunction)
                .forEach(route ->
                        LoggerFactory.getLogger(Router.class).info("Unregister route {}", route)
                ));
    }

    private <T extends Annotation, R extends Route> void registerMethod(HttpServerRoutes httpServerRoutes, ReactiveHandler reactiveHandler, HandlerSupportFunction<T, R> handlerSupportFunction) {
        getAnnotatedRoutes(reactiveHandler, handlerSupportFunction)
                .forEach(route -> handlerSupportFunction.register(httpServerRoutes, reactiveHandler, route));
    }

    private <T extends Annotation, R extends Route> Stream<R> getAnnotatedRoutes(ReactiveHandler reactiveHandler, HandlerSupportFunction<T, R> handlerSupportFunction) {
        return Stream.of(reactiveHandler.getClass().getDeclaredMethods())
                .map(m -> Maps.immutableEntry(m.getAnnotation(handlerSupportFunction.supports()), m))
                .filter(e -> e.getKey() != null)
                .flatMap(methodEntry ->
                        handlerSupportFunction.buildRoutes(methodEntry.getKey(), methodEntry.getValue())
                                .stream()
                )
                .sorted(ROUTE_ORDER);
    }

    private Mono<Result> notFound(Request request) {
        List<Route> routes = reactiveHandlers.stream()
                .flatMap(reactiveHandler -> handlerSupportFunctions.stream()
                        .flatMap(handlerSupportFunction -> getAnnotatedRoutes(reactiveHandler, handlerSupportFunction)))
                .collect(toList());

        ImmutableMap<String, String> parameters = ImmutableMap.of("requestMethod", request.method().name(), "requestUri", request.uri(), "routes", formatRoutes(routes));
        return TEMPLATE_ENGINE.process(NOT_FOUND_TEMPLATE, parameters)
                .map(page -> Result.<String>builder()
                        .status(404, String.format("'%s' not found", request.uri()))
                        .header(HttpHeaderNames.CONTENT_TYPE, "text/plain")
                        .data(Mono.just(page), String.class)
                        .build());
    }

}