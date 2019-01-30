package com.liveaction.reactiff.server.netty;

import com.liveaction.reactiff.server.netty.internal.ReactiveHttpServerBuilder;

import java.io.Closeable;

public interface ReactiveHttpServer extends Closeable {

    static ReactiveHttpServerBuilder create() {
        return new ReactiveHttpServerBuilder();
    }

    void start();

    void close();

    int port();

}
