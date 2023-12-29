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
import io.netty.handler.logging.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.scheduler.Scheduler;
import reactor.netty.DisposableServer;
import reactor.netty.channel.ChannelMetricsRecorder;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

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
                                  ChannelMetricsRecorder channelMetricsRecorder,
                                  boolean wiretap,
                                  boolean compress,
                                  boolean displayRoutes,
                                  boolean writeErrorStacktrace,
                                  Function<HttpServer, HttpServer> configuration,
                                  Optional<String> originHeader) {
        this.host = host;
        this.port = port;
        this.protocols = protocols;
        this.ioExecutor = ioExecutor;
        this.workScheduler = workScheduler;
        this.router = new Router(codecManager,
                paramConverter,
                this::chain,
                writeErrorStacktrace,
                executionContextServiceManager,
                displayRoutes,
                workScheduler,
                originHeader);
        this.httpServer = configuration.apply(createServer(wiretap, compress, channelMetricsRecorder));
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
        LOGGER.debug("ReactiveFilter {}(rank={}) added", reactiveFilter, reactiveFilter.filterRank());
    }

    @Override
    public void removeReactiveFilter(ReactiveFilter reactiveFilter) {
        this.reactiveFilters.remove(reactiveFilter);
        LOGGER.debug("ReactiveFilter {}(rank={}) removed", reactiveFilter, reactiveFilter.filterRank());
    }

    @Override
    public void addReactiveHandler(ReactiveHandler reactiveHandler) {
        router.addReactiveHander(reactiveHandler);
        LOGGER.debug("ReactiveHandler {}(rank={}) added", reactiveHandler.getClass().getName(), reactiveHandler.handlerRank());
    }

    @Override
    public void removeReactiveHandler(ReactiveHandler reactiveHandler) {
        router.removeReactiveHander(reactiveHandler);
        LOGGER.debug("ReactiveHandler {}(rank={}) removed", reactiveHandler.getClass().getName(), reactiveHandler.handlerRank());
    }

    @Override
    public void setOriginsToMonitor(Set<String> originsToMonitor) {
        router.setOriginsToMonitor(originsToMonitor);
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

    private HttpServer createServer(boolean wiretap, boolean compress, ChannelMetricsRecorder channelMetricsRecorder) {
        HttpServer httpServer = HttpServer.create()
                .compress(compress)
                .protocol(protocols.toArray(new HttpProtocol[0]))
                .host(host);
        if (wiretap) {
            httpServer = httpServer.wiretap(HttpServer.class.getName(), LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL);
        }

        if (channelMetricsRecorder != null) {
            httpServer = httpServer.metrics(true, () -> channelMetricsRecorder);
        }

        if (ioExecutor != null) {
            httpServer = httpServer.runOn(new EpollEventLoopGroup(0, ioExecutor));
        }

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
