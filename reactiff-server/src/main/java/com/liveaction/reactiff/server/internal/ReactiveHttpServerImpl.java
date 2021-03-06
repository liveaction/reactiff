package com.liveaction.reactiff.server.internal;

import com.google.common.collect.ImmutableList;
import com.liveaction.reactiff.api.codec.CodecManager;
import com.liveaction.reactiff.api.server.FilterChain;
import com.liveaction.reactiff.api.server.ReactiveFilter;
import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.api.server.route.Route;
import com.liveaction.reactiff.server.ReactiveHttpServer;
import com.liveaction.reactiff.server.context.ExecutionContextService;
import com.liveaction.reactiff.server.internal.context.ExecutionContextServiceManager;
import com.liveaction.reactiff.server.internal.param.ParamConverter;
import com.liveaction.reactiff.server.internal.utils.FilterUtils;
import com.liveaction.reactiff.server.param.converter.ParamTypeConverter;
import io.netty.channel.epoll.EpollEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.scheduler.Scheduler;
import reactor.netty.DisposableServer;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public final class ReactiveHttpServerImpl implements ReactiveHttpServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveHttpServerImpl.class);
    private static final Comparator<ReactiveFilter> FILTER_COMPARATOR = Comparator.reverseOrder();

    private final Set<ReactiveFilter> reactiveFilters = Collections.synchronizedSortedSet(new TreeSet<>(FILTER_COMPARATOR));
    private final String host;
    private final int port;
    private final Collection<HttpProtocol> protocols;
    private final Executor ioExecutor;
    private final Scheduler workScheduler;

    private final HttpServer httpServer;
    private final Router router;
    private final ParamConverter paramConverter = new ParamConverter(ImmutableList.of());
    private final ExecutionContextServiceManager executionContextServiceManager = new ExecutionContextServiceManager();

    private DisposableServer disposableServer;

    public ReactiveHttpServerImpl(String host,
                                  int port,
                                  Collection<HttpProtocol> protocols,
                                  CodecManager codecManager,
                                  Executor ioExecutor,
                                  Scheduler workScheduler,
                                  boolean wiretap,
                                  boolean compress,
                                  boolean displayRoutes,
                                  boolean writeErrorStacktrace) {
        this.host = host;
        this.port = port;
        this.protocols = protocols;
        this.ioExecutor = ioExecutor;
        this.workScheduler = workScheduler;
        this.router = new Router(codecManager, paramConverter, this::chain, writeErrorStacktrace, executionContextServiceManager, displayRoutes, workScheduler);
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
    public List<Route> routes() {
        return router.routes();
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

    @Override
    public void addParamTypeConverter(ParamTypeConverter<?> paramTypeConverter) {
        paramConverter.addConverter(paramTypeConverter);
    }

    @Override
    public void removeParamTypeConverter(ParamTypeConverter<?> paramTypeConverter) {
        paramConverter.removeConverter(paramTypeConverter);
    }

    @Override
    public void addExecutionContextService(ExecutionContextService executionContextService) {
        executionContextServiceManager.addExecutionContextService(executionContextService);
    }

    @Override
    public void removeExecutionContextService(ExecutionContextService executionContextService) {
        executionContextServiceManager.removeExecutionContextService(executionContextService);
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
                .tcpConfiguration(tcpServer -> {
                    if (ioExecutor != null) {
                        tcpServer = tcpServer.runOn(new EpollEventLoopGroup(0, ioExecutor));
                    }
                    return tcpServer;
                })
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
