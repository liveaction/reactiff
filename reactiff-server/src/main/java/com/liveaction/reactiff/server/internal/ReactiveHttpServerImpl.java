package com.liveaction.reactiff.server.internal;

import com.liveaction.reactiff.api.codec.CodecManager;
import com.liveaction.reactiff.api.server.FilterChain;
import com.liveaction.reactiff.api.server.ReactiveFilter;
import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.server.ReactiveHttpServer;
import com.liveaction.reactiff.server.internal.utils.FilterUtils;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.DisposableServer;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.HttpResources;
import reactor.netty.http.server.HttpServer;
import reactor.netty.resources.LoopResources;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public final class ReactiveHttpServerImpl implements ReactiveHttpServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveHttpServerImpl.class);
    private static final Comparator<ReactiveFilter> FILTER_COMPARATOR = Comparator.reverseOrder();

    private final Set<ReactiveFilter> reactiveFilters = Collections.synchronizedSortedSet(new TreeSet<>(FILTER_COMPARATOR));
    private final String host;
    private final int port;
    private final Collection<HttpProtocol> protocols;
    private final Executor executor;

    private final HttpServer httpServer;
    private final Router router;

    private DisposableServer disposableServer;

    public ReactiveHttpServerImpl(String host,
                                  int port,
                                  Collection<HttpProtocol> protocols,
                                  CodecManager codecManager,
                                  Executor executor,
                                  boolean wiretap,
                                  boolean compress,
                                  boolean writeErrorStacktrace) {
        this.host = host;
        this.port = port;
        this.protocols = protocols;
        this.executor = executor;
        this.router = new Router(codecManager, this::chain, writeErrorStacktrace);
        this.httpServer = createServer(wiretap, compress);
    }

    @Override
    public boolean isStarted() {
        return disposableServer != null;
    }

    @Override
    public void start() {
        disposableServer = httpServer.bindNow();
        LOGGER.info("Server started on http://{}:{}", host, disposableServer.port());
    }

    @Override
    public void startAndWait() {
        httpServer.bindUntilJavaShutdown(Duration.ofSeconds(5), null);
    }

    @Override
    public void startAndWait(Consumer onStart) {
        httpServer.bindUntilJavaShutdown(Duration.ofSeconds(5), onStart);
    }

    @Override
    public void close() {
        disposableServer.disposeNow();
        disposableServer = null;
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
        LOGGER.info("ReactiveHandler {}(rank={}) added", reactiveHandler.getClass().getName(), reactiveHandler.handlerRank());
    }

    @Override
    public void removeReactiveHandler(ReactiveHandler reactiveHandler) {
        router.removeReactiveHander(reactiveHandler);
        LOGGER.info("ReactiveHandler {}(rank={}) removed", reactiveHandler.getClass().getName(), reactiveHandler.handlerRank());
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
                .tcpConfiguration(tcpServer ->
                        tcpServer.bootstrap(serverBootstrap -> {
                            if (executor == null) {
                                return serverBootstrap;
                            } else {
                                // From Reactor-Netty-tcp HttpServerBind
                                LoopResources loops = HttpResources.get();

                                boolean useNative =
                                        LoopResources.DEFAULT_NATIVE;

                                EventLoopGroup elg = loops.onServer(useNative);

                                return serverBootstrap.group(new EpollEventLoopGroup(0, executor))
                                        .channel(loops.onServerChannel(elg));
                            }
                        }))
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
