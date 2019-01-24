package com.liveaction.reactiff.server.netty.internal;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.liveaction.reactiff.codec.CodecManager;
import com.liveaction.reactiff.server.netty.NettyServer;
import com.liveaction.reactiff.server.netty.ReactiveFilter;
import com.liveaction.reactiff.server.netty.ReactiveHandler;
import com.liveaction.reactiff.server.netty.Request;
import com.liveaction.reactiff.server.netty.Result;
import com.liveaction.reactiff.server.netty.internal.support.HandlerSupportFunction;
import com.liveaction.reactiff.server.netty.internal.support.RequestMappingSupport;
import com.liveaction.reactiff.server.netty.internal.support.WsMappingSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRoutes;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

public final class NettyServerImpl implements NettyServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyServerImpl.class);
    private static final Comparator<ReactiveFilter> FILTER_COMPARATOR = Comparator.reverseOrder();

    private final Set<ReactiveHandler> reactiveHandlers;
    private final Set<ReactiveFilter> reactiveFilters;
    private final String host;
    private final int port;
    private final Collection<HttpProtocol> protocols;

    private final ImmutableSet<HandlerSupportFunction<?>> handlerSupportFunctions;
    private final HttpServer httpServer;
    private final CodecManager codecManager;

    private DisposableServer disposableServer;

    NettyServerImpl(String host,
                    int port,
                    Collection<HttpProtocol> protocols,
                    Collection<ReactiveFilter> filters,
                    Collection<ReactiveHandler> handlers,
                    CodecManager codecManager) {
        this.host = host;
        this.port = port;
        this.protocols = protocols;
        this.codecManager = codecManager;

        Set<ReactiveFilter> reactiveFilters = Collections.synchronizedSortedSet(new TreeSet<>(FILTER_COMPARATOR));
        reactiveFilters.addAll(filters);
        this.reactiveFilters = reactiveFilters;

        Set<ReactiveHandler> reactiveHandlers = Collections.synchronizedSortedSet(new TreeSet<>());
        reactiveHandlers.addAll(handlers);
        this.reactiveHandlers = reactiveHandlers;

        this.handlerSupportFunctions = ImmutableSet.of(
                new RequestMappingSupport(codecManager, reactiveFilters),
                new WsMappingSupport()
        );
        this.httpServer = createServer();
    }

    @Override
    public void start() {
        disposableServer = httpServer.bindNow();
        LOGGER.info("Server started on http://{}:{}", host, disposableServer.port());
    }

    @Override
    public void close() {
        disposableServer.disposeNow();
    }

    @Override
    public int port() {
        return disposableServer.port();
    }

    private HttpServer createServer() {
        HttpServer httpServer = HttpServer.create()
                .protocol(protocols.toArray(new HttpProtocol[0]))
                .host(host);
        if (port != -1) {
            httpServer = httpServer.port(port);
        }

        HttpServerRoutes httpServerRoutes = HttpServerRoutes.newRoutes();

        handlerSupportFunctions.forEach(handlerSupportFunction ->
                reactiveHandlers.forEach(reactiveHandler ->
                        registerMethod(httpServerRoutes, reactiveHandler, handlerSupportFunction)
                )
        );
        httpServerRoutes.route(httpServerRequest -> true, (req, res) -> FilterUtils.applyFilters(req, res, codecManager, reactiveFilters, this::notFound, Optional.empty()));

        return httpServer.handle(httpServerRoutes);
    }

    private <T extends Annotation> void registerMethod(HttpServerRoutes httpServerRoutes, ReactiveHandler reactiveHandler, HandlerSupportFunction<T> handlerSupportFunction) {
        Stream.of(reactiveHandler.getClass().getDeclaredMethods())
                .map(m -> Maps.immutableEntry(m.getAnnotation(handlerSupportFunction.supports()), m))
                .filter(e -> e.getKey() != null)
                .sorted(Comparator.comparingInt(o -> handlerSupportFunction.rank(o.getKey())))
                .forEach(e -> handlerSupportFunction.register(httpServerRoutes, e.getKey(), reactiveHandler, e.getValue()));
    }

    private Mono<Result> notFound(Request request) {
        return Mono.just(Result.withStatus(404, "Not found"));
    }

}
