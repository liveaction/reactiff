package com.liveaction.reactiff.server.netty;

import com.liveaction.reactiff.codec.CodecManager;
import com.liveaction.reactiff.server.netty.internal.ReactiveHttpServerBuilder;
import reactor.netty.http.HttpProtocol;

import java.io.Closeable;

public interface ReactiveHttpServer extends Closeable {

    interface Builder {

        Builder host(String host);

        Builder port(int port);

        Builder protocols(HttpProtocol... protocols);

        Builder filters(Iterable<ReactiveFilter> filters);

        Builder filter(ReactiveFilter filter);

        Builder handler(ReactiveHandler handler);

        Builder codecManager(CodecManager codecManager);

        ReactiveHttpServer build();

    }

    static Builder create() {
        return new ReactiveHttpServerBuilder();
    }

    void start();

    void close();

    int port();

    void addReactiveFilter(ReactiveFilter reactiveFilter);

    void removeReactiveFilter(ReactiveFilter reactiveFilter);

    void addReactiveHandler(ReactiveHandler reactiveHandler);

    void removeReactiveHandler(ReactiveHandler reactiveHandler);

}
