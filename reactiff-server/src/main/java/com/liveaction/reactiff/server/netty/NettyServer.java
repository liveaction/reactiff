package com.liveaction.reactiff.server.netty;

import com.google.common.collect.Maps;
import com.liveaction.reactiff.codec.CodecManager;
import com.liveaction.reactiff.server.netty.annotation.RequestMapping;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
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
        this.httpServerRoutes = HttpServerRoutes.newRoutes();

        reactiveHandlers.forEach(this::registerRoutes);

    }

    private void registerRoutes(ReactiveHandler reactiveHandler) {
        Stream.of(reactiveHandler.getClass().getDeclaredMethods())
                .map(m -> Maps.immutableEntry(m.getAnnotation(RequestMapping.class), m))
                .filter(e -> e.getKey() != null)
                .sorted(Comparator.comparingInt(o -> o.getKey().rank()))
                .forEach(e -> {
                    RequestMapping annotation = e.getKey();
                    Method m = e.getValue();
                    LOGGER.info("Registered route {} : '{}' -> {}", annotation.method(), annotation.path(), m);
                    BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> onRequest = (req, res) -> {
                        try {
                            LOGGER.info("invoke path {} with {}", annotation.path(), req.uri());
                            Publisher<?> invoke = (Publisher<?>) m.invoke(reactiveHandler, req);
                            return res.send(codecManager.encode(req.requestHeaders(), res, invoke));
                        } catch (IllegalAccessException | InvocationTargetException error) {
                            return Mono.error(error);
                        }
                    };
                    switch (annotation.method()) {
                        case GET:
                            httpServerRoutes.get(annotation.path(), onRequest);
                            break;
                        case POST:
                            httpServerRoutes.post(annotation.path(), onRequest);
                        case OPTIONS:
                            httpServerRoutes.options(annotation.path(), onRequest);
                        case HEAD:
                            httpServerRoutes.head(annotation.path(), onRequest);
                        case PUT:
                            httpServerRoutes.put(annotation.path(), onRequest);
                        case DELETE:
                            httpServerRoutes.delete(annotation.path(), onRequest);
                            break;
                        default:
                            LOGGER.warn("Unknown HttpMethod: {}", annotation.method());
                    }
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
