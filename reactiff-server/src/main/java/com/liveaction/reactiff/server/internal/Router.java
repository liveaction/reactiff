package com.liveaction.reactiff.server.internal;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.liveaction.reactiff.api.codec.CodecManager;
import com.liveaction.reactiff.api.server.FilterChain;
import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.api.server.Request;
import com.liveaction.reactiff.api.server.Result;
import com.liveaction.reactiff.api.server.route.Route;
import com.liveaction.reactiff.server.context.ExecutionContextService;
import com.liveaction.reactiff.server.internal.param.ParamConverter;
import com.liveaction.reactiff.server.internal.support.HandlerSupportFunction;
import com.liveaction.reactiff.server.internal.support.RequestMappingSupport;
import com.liveaction.reactiff.server.internal.support.WsMappingSupport;
import com.liveaction.reactiff.server.internal.template.TemplateEngineImpl;
import com.liveaction.reactiff.server.internal.utils.FilterUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;

import java.lang.annotation.Annotation;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.liveaction.reactiff.api.server.utils.FormatUtils.formatRoutes;
import static java.util.stream.Collectors.toList;

public final class Router implements BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> {

    private static final Comparator<Route> ROUTE_ORDER = Comparator.comparingInt(Route::rank)
            .thenComparing(Route::hasUriParam)
            .thenComparing(Route::descriptor)
            .thenComparing(Route::path)
            .thenComparing(r -> r.handlerMethod().getName());

    private static final String NOT_FOUND_TEMPLATE = "/templates/not-found.txt";
    private static final TemplateEngineImpl TEMPLATE_ENGINE = new TemplateEngineImpl();

    private final ImmutableSet<HandlerSupportFunction<? extends Annotation, ? extends Route>> handlerSupportFunctions;

    private final Set<ReactiveHandler> reactiveHandlers = new ConcurrentSkipListSet<>();

    private final CodecManager codecManager;
    private final Function<FilterChain, FilterChain> filterFunction;
    private HttpServerRoutes httpServerRoutes = HttpServerRoutes.newRoutes();

    private final boolean writeErrorStacktrace;
    private final boolean displayRoutes;

    private static final Logger LOGGER = LoggerFactory.getLogger(Router.class);


    public Router(CodecManager codecManager, ParamConverter paramConverter, Function<FilterChain, FilterChain> filterFunction,
                  boolean writeErrorStacktrace, ExecutionContextService executionContextService, boolean displayRoutes) {
        this.codecManager = codecManager;
        this.filterFunction = filterFunction;
        this.handlerSupportFunctions = ImmutableSet.of(
                new RequestMappingSupport(codecManager, paramConverter, filterFunction, writeErrorStacktrace, executionContextService),
                new WsMappingSupport(filterFunction, codecManager)
        );
        this.writeErrorStacktrace = writeErrorStacktrace;
        this.displayRoutes = displayRoutes;
    }

    public void addReactiveHander(ReactiveHandler reactiveHandler) {
        LOGGER.info("Adding Controller {}", reactiveHandler.getClass().getName());
        this.reactiveHandlers.add(reactiveHandler);
        try {
            updateRoutes();
        } catch (Throwable t) { // catch all errors to let other handlers be registered
            LOGGER.error(String.format("Error occurred while registering routes of handler %s", reactiveHandler.getClass().getSimpleName()), t);
            removeReactiveHander(reactiveHandler);
        }
    }

    public void removeReactiveHander(ReactiveHandler reactiveHandler) {
        LOGGER.info("Removing Controller {}", reactiveHandler.getClass().getName());
        this.reactiveHandlers.remove(reactiveHandler);
        updateRoutes();
    }

    @Override
    public Publisher<Void> apply(HttpServerRequest request, HttpServerResponse response) {
        return httpServerRoutes.apply(request, response);
    }

    private void updateRoutes() {
        HttpServerRoutes httpServerRoutes = HttpServerRoutes.newRoutes();

        reactiveHandlers.forEach(rh -> registerMethod(httpServerRoutes, rh));
        httpServerRoutes.route(httpServerRequest -> true, (req, res) -> FilterUtils.applyFilters(req, res, codecManager, filterFunction, this::notFound, Optional.empty(), writeErrorStacktrace));
        this.httpServerRoutes = httpServerRoutes;
    }

    private void registerMethod(HttpServerRoutes httpServerRoutes, ReactiveHandler reactiveHandler) {
        getRoutes(reactiveHandler)
                .forEach(handledRoute -> {
                    LoggerFactory.getLogger(Router.class).debug("Register route {}", handledRoute.route);
                    handledRoute.register(httpServerRoutes, reactiveHandler);
                });
    }

    private Stream<? extends HandledRoute<? extends Annotation, ? extends Route>> getRoutes(ReactiveHandler reactiveHandler) {
        return handlerSupportFunctions.stream()
                .flatMap(handlerSupportFunction -> getHandledRoutes(reactiveHandler, handlerSupportFunction))
                .sorted((r1, r2) -> ROUTE_ORDER.compare(r1.route, r2.route));
    }

    private <T extends Annotation, R extends Route> Stream<HandledRoute<T, R>> getHandledRoutes(ReactiveHandler reactiveHandler, HandlerSupportFunction<T, R> handlerSupportFunction) {
        return getAnnotatedRoutes(reactiveHandler, handlerSupportFunction)
                .map(route -> new HandledRoute<>(route, handlerSupportFunction));
    }

    private <T extends Annotation, R extends Route> Stream<R> getAnnotatedRoutes(ReactiveHandler reactiveHandler, HandlerSupportFunction<T, R> handlerSupportFunction) {
        return Stream.of(reactiveHandler.getClass().getDeclaredMethods())
                .map(m -> Maps.immutableEntry(m.getAnnotation(handlerSupportFunction.supports()), m))
                .filter(e -> e.getKey() != null)
                .flatMap(methodEntry ->
                        handlerSupportFunction.buildRoutes(methodEntry.getKey(), methodEntry.getValue())
                                .stream()
                );
    }

    private Mono<Result> notFound(Request request) {
        List<Route> routes = routes();
        String routesStr = displayRoutes ? formatRoutes(routes) : "";

        ImmutableMap<String, String> parameters = ImmutableMap.of("requestMethod", request.method().name(), "requestUri", request.uri(), "routes", routesStr);
        return TEMPLATE_ENGINE.process(NOT_FOUND_TEMPLATE, parameters)
                .map(page -> Result.<String>builder()
                        .status(404, String.format("'%s' not found", request.uri()))
                        .data(Mono.just(page), String.class)
                        .build());
    }

    public List<Route> routes() {
        return reactiveHandlers.stream()
                .flatMap(this::getRoutes)
                .map(hr -> hr.route)
                .collect(toList());
    }

    private static final class HandledRoute<T extends Annotation, R extends Route> {
        final R route;
        final HandlerSupportFunction<T, R> handlerSupportFunction;

        private HandledRoute(R route, HandlerSupportFunction<T, R> handlerSupportFunction) {
            this.route = route;
            this.handlerSupportFunction = handlerSupportFunction;
        }

        void register(HttpServerRoutes httpServerRoutes, ReactiveHandler reactiveHandler) {
            handlerSupportFunction.register(httpServerRoutes, reactiveHandler, route);
        }
    }


}