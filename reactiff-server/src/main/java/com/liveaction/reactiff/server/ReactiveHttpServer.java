package com.liveaction.reactiff.server;

import com.liveaction.reactiff.api.codec.CodecManager;
import com.liveaction.reactiff.api.server.ReactiveFilter;
import com.liveaction.reactiff.api.server.ReactiveHandler;
import reactor.netty.http.HttpProtocol;

import java.io.Closeable;
import java.util.function.Consumer;

public interface ReactiveHttpServer extends Closeable {

    interface Builder {

        Builder host(String host);

        Builder port(int port);

        Builder protocols(HttpProtocol... protocols);

        Builder filters(Iterable<ReactiveFilter> filters);

        Builder filter(ReactiveFilter filter);

        Builder handler(ReactiveHandler handler);

        Builder codecManager(CodecManager codecManager);

        Builder wiretap(boolean wiretap);

        Builder compress(boolean compress);

        Builder writeErrorStacktrace(boolean writeErrorStacktrace);

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

}
