package com.liveaction.reactiff.server;

import com.liveaction.reactiff.api.codec.CodecManager;
import com.liveaction.reactiff.api.server.ReactiveFilter;
import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.api.server.route.Route;
import com.liveaction.reactiff.server.context.ExecutionContextService;
import com.liveaction.reactiff.server.param.converter.ParamTypeConverter;
import reactor.core.scheduler.Scheduler;
import reactor.netty.channel.ChannelMetricsRecorder;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;

import java.io.Closeable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ReactiveHttpServer extends Closeable {

    void addParamTypeConverter(ParamTypeConverter<?> paramTypeConverter);

    void removeParamTypeConverter(ParamTypeConverter<?> paramTypeConverter);

    List<Route> routes();

    void addExecutionContextService(ExecutionContextService executionContextService);

    void removeExecutionContextService(ExecutionContextService executionContextService);

    interface Builder {

        Builder host(String host);

        Builder port(int port);

        Builder protocols(HttpProtocol... protocols);

        Builder filters(Iterable<ReactiveFilter> filters);

        Builder filter(ReactiveFilter filter);

        Builder filter(ReactiveFilter filter, boolean add);

        Builder handler(ReactiveHandler handler);

        Builder handler(ReactiveHandler handler, boolean add);

        Builder executionContextService(ExecutionContextService executionContextService);

        Builder executionContextService(ExecutionContextService executionContextService, boolean add);

        Builder converter(ParamTypeConverter<?> converter);

        Builder converter(ParamTypeConverter<?> converter, boolean add);

        Builder codecManager(CodecManager codecManager);

        Builder ioExecutor(Executor ioExecutor);

        Builder workScheduler(Scheduler workScheduler);

        Builder metrics(ChannelMetricsRecorder channelMetricsRecorder);

        Builder wiretap(boolean wiretap);

        Builder displayRoutes(boolean displayRoutes);

        Builder compress(boolean compress);

        Builder writeErrorStacktrace(boolean writeErrorStacktrace);

        Builder configure(Function<HttpServer, HttpServer> configuration);
        Builder originHeader(String originHeader);

        ReactiveHttpServer build();

    }

    static Builder create() {
        return new ReactiveHttpServerBuilder();
    }

    boolean isStarted();

    void start();

    void startAndWait();

    void startAndWait(Consumer onStart);

    void close();

    int port();

    void addReactiveFilter(ReactiveFilter reactiveFilter);

    void removeReactiveFilter(ReactiveFilter reactiveFilter);

    void addReactiveHandler(ReactiveHandler reactiveHandler);

    void removeReactiveHandler(ReactiveHandler reactiveHandler);

    void setOriginsToMonitor(Set<String> originToMonitor);
}
