package com.liveaction.reactiff.server.internal;

import com.liveaction.reactiff.api.codec.CodecManager;
import com.liveaction.reactiff.api.server.FilterChain;
import com.liveaction.reactiff.api.server.ReactiveFilter;
import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.server.ReactiveHttpServer;
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
                           CodecManager codecManager, boolean wiretap, boolean compress) {
        this.host = host;
        this.port = port;
        this.protocols = protocols;
        this.router = new Router(codecManager, this::chain);
        this.httpServer = createServer(wiretap, compress);
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
        LOGGER.info("ReactiveFilter {}(rank={}) added", reactiveFilter, reactiveFilter.filterRank());
    }

    @Override
    public void removeReactiveFilter(ReactiveFilter reactiveFilter) {
        this.reactiveFilters.remove(reactiveFilter);
        LOGGER.info("ReactiveFilter {}(rank={}) removed", reactiveFilter, reactiveFilter.filterRank());
    }

    @Override
    public void addReactiveHandler(ReactiveHandler reactiveHandler) {
        router.addReactiveHander(reactiveHandler);
    }

    @Override
    public void removeReactiveHandler(ReactiveHandler reactiveHandler) {
        router.removeReactiveHander(reactiveHandler);
    }

    private HttpServer createServer(boolean wiretap, boolean compress) {
        HttpServer httpServer = HttpServer.create();
        if (wiretap) {
            httpServer = httpServer.wiretap(true);
        }
        if (compress) {
            httpServer = httpServer.compress(true);
        }
        httpServer = httpServer
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
