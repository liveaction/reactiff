package com.liveaction.reactiff.server.netty;

import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.codec.CodecManager;
import com.liveaction.reactiff.server.netty.annotation.RequestMapping;
import com.liveaction.reactiff.server.netty.internal.RequestImpl;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.NettyOutbound;
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
                .host(host);
        if (port != -1) {
            httpServer = httpServer.port(port);
        }
        registerAllRoutes();

        httpServer = httpServer.handle(this.httpServerRoutes);
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
                    FilterChain route = (req, res) -> {
                        try {
                            LOGGER.info("invoke path {} with {}", annotation.path(), req.uri());
                            TypeToken<?> returnType = TypeToken.of(m.getGenericReturnType());
                            Request request = new RequestImpl(req, codecManager);
                            Object rawResult = m.invoke(reactiveHandler, request);
                            return toResult(returnType, rawResult);
                        } catch (IllegalAccessException | InvocationTargetException error) {
                            return Mono.error(error);
                        }
                    };
                    BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> onRequest = (req, res) -> applyFilters(req, res, route);
                    switch (annotation.method()) {
                        case GET:
                            httpServerRoutes.get(annotation.path(), onRequest);
                            break;
                        case POST:
                            httpServerRoutes.post(annotation.path(), onRequest);
                            break;
                        case OPTIONS:
                            httpServerRoutes.options(annotation.path(), onRequest);
                            break;
                        case HEAD:
                            httpServerRoutes.head(annotation.path(), onRequest);
                            break;
                        case PUT:
                            httpServerRoutes.put(annotation.path(), onRequest);
                            break;
                        case DELETE:
                            httpServerRoutes.delete(annotation.path(), onRequest);
                            break;
                        default:
                            LOGGER.warn("Unknown HttpMethod: {}", annotation.method());
                            break;
                    }
                });
    }

    private Mono<Result<?>> toResult(TypeToken<?> returnType, Object result) {
        Class<?> rawType = returnType.getRawType();

        if (Mono.class.isAssignableFrom(rawType)) {
            TypeToken<?> paramType = returnType.resolveType(Mono.class.getTypeParameters()[0]);
            if (Result.class.isAssignableFrom(paramType.getClass())) {
                return (Mono<Result<?>>) result;
            }
            Mono<?> publisher = (Mono) result;
            return publisher.flatMap(mono -> Mono.just(Result.ok(Mono.just(mono))));

        } else if (Publisher.class.isAssignableFrom(rawType)) {
            TypeToken<?> paramType = returnType.resolveType(Publisher.class.getTypeParameters()[0]);
            if (Result.class.isAssignableFrom(paramType.getClass())) {
                return Mono.from((Publisher<Result<?>>) result);
            }
            Publisher<?> publisher = (Publisher) result;
            return Mono.just(Result.ok(publisher));

        } else if (Result.class.isAssignableFrom(rawType)) {
            Result<?> httpResult = (Result<?>) result;
            return Mono.just(httpResult);

        } else {
            return Mono.just(Result.ok(Mono.just(result)));
        }
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

    @Override
    public void close() {
        disposableServer.disposeNow();
    }

    public int port() {
        return disposableServer.port();
    }

}
