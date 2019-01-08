package com.liveaction.reactiff.server.netty;

import com.google.common.collect.Maps;
import com.liveaction.reactiff.codec.CodecManager;
import com.liveaction.reactiff.server.netty.annotation.Get;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

public final class NettyServer implements Closeable {
    public static final Logger LOGGER = LoggerFactory.getLogger(NettyServer.class);
    private final Set<ReactiveFilter> reactiveFilters = Collections.synchronizedSortedSet(new TreeSet<>((o1, o2) -> o2.compareTo(o1)));
    private final Set<ReactiveHandler> reactiveHandlers = Collections.synchronizedSortedSet(new TreeSet<>());
    private final String host;
    private final int port;
    private final Collection<HttpProtocol> protocols;
    private final CodecManager codecManager;
    private DisposableServer disposableServer;
    private HttpServerRoutes httpServerRoutes;

    NettyServer(String host,
                int port,
                Collection<HttpProtocol> protocols,
                Collection<ReactiveFilter> filters,
                Collection<ReactiveHandler> handlers,
                CodecManager codecManager) {
        this.host = host;
        this.port = port;
        this.protocols = protocols;
        this.codecManager = codecManager;
        this.reactiveFilters.addAll(filters);
        this.reactiveHandlers.addAll(handlers);
    }

    public NettyServer start() {
        HttpServer httpServer = HttpServer.create()
                .protocol(protocols.toArray(new HttpProtocol[0]))
                .host(host)
                .port(port);
        registerAllRoutes();

        httpServer = httpServer.handle(this::handle);
        disposableServer = httpServer.bindNow();
        return this;
    }

    private void registerAllRoutes() {
        HttpServerRoutes httpServerRoutes = HttpServerRoutes.newRoutes();
        this.httpServerRoutes = httpServerRoutes;

        reactiveHandlers.forEach(this::registerRoutes);

    }

    private void registerRoutes(ReactiveHandler reactiveHandler) {
        Stream.of(reactiveHandler.getClass().getDeclaredMethods())
                .map(m -> Maps.immutableEntry(m.getAnnotation(Get.class), m))
                .filter(e -> e.getKey() != null)
                .sorted(Comparator.comparingInt(o -> o.getKey().rank()))
                .forEach(e -> {
                    Get annotation = e.getKey();
                    Method m = e.getValue();
                    LOGGER.info("Registered route : '{}' -> {}", annotation.path(), m);
                    httpServerRoutes.get(annotation.path(), (req, res) -> {
                        try {
                            Publisher<?> invoke = (Publisher<?>) m.invoke(reactiveHandler, req);
                            return res.send(codecManager.encode(req.requestHeaders(), res, invoke));
                        } catch (IllegalAccessException | InvocationTargetException error) {
                            return Mono.error(error);
                        }
                    });
                });
    }

    private Publisher<Void> handle(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
        FilterChain filterChain = this.handleRoutes();
        for (ReactiveFilter element : this.reactiveFilters) {
            filterChain = chain(element, filterChain);
        }
        return filterChain.chain(httpServerRequest, httpServerResponse);
    }

    private FilterChain chain(ReactiveFilter element, FilterChain filterChain) {
        return (httpServerRequest, httpServerResponse) -> element.filter(httpServerRequest, httpServerResponse, filterChain);
    }

    private FilterChain handleRoutes() {
        return (httpServerRequest, httpServerResponse) -> Mono.from(httpServerRoutes.apply(httpServerRequest, httpServerResponse));
    }

    @Override
    public void close() {
        disposableServer.disposeNow();
    }

    public int port() {
        return disposableServer.port();
    }

}
