package com.liveaction.reactiff.server.netty.internal;

import com.liveaction.reactiff.codec.CodecManager;
import com.liveaction.reactiff.server.netty.FilterChain;
import com.liveaction.reactiff.server.netty.ReactiveFilter;
import com.liveaction.reactiff.server.netty.ReactiveHandler;
import com.liveaction.reactiff.server.netty.ReactiveHttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.DisposableServer;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public final class ReactiveHttpServerImpl implements ReactiveHttpServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveHttpServerImpl.class);
    private static final Comparator<ReactiveFilter> FILTER_COMPARATOR = Comparator.reverseOrder();

    private final Set<ReactiveFilter> reactiveFilters = Collections.synchronizedSortedSet(new TreeSet<>(FILTER_COMPARATOR));
    private final String host;
    private final int port;
    private final Collection<HttpProtocol> protocols;

    private final HttpServer httpServer;
    private final Router router;

    private DisposableServer disposableServer;

    ReactiveHttpServerImpl(String host,
                           int port,
                           Collection<HttpProtocol> protocols,
                           CodecManager codecManager) {
        this.host = host;
        this.port = port;
        this.protocols = protocols;

        this.router = new Router(codecManager, this::chain);
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

    @Override
    public void addReactiveFilter(ReactiveFilter reactiveFilter) {
        this.reactiveFilters.add(reactiveFilter);
    }

    @Override
    public void removeReactiveFilter(ReactiveFilter reactiveFilter) {
        this.reactiveFilters.remove(reactiveFilter);
    }

    @Override
    public void addReactiveHandler(ReactiveHandler reactiveHandler) {
        router.addReactiveHander(reactiveHandler);
    }

    @Override
    public void removeReactiveHandler(ReactiveHandler reactiveHandler) {
        router.removeReactiveHander(reactiveHandler);
    }

    private HttpServer createServer() {
        HttpServer httpServer = HttpServer.create()
                .protocol(protocols.toArray(new HttpProtocol[0]))
                .host(host);
        if (port != -1) {
            httpServer = httpServer.port(port);
        }

        return httpServer.handle(router);
    }

    private FilterChain chain(FilterChain inputChain) {
        FilterChain filterChain = inputChain;
        for (ReactiveFilter element : reactiveFilters) {
            filterChain = FilterUtils.chain(element, filterChain);
        }
        return filterChain;
    }

}
