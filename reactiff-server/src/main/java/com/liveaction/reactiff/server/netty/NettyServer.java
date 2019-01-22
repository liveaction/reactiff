package com.liveaction.reactiff.server.netty;

import com.liveaction.reactiff.server.netty.internal.NettyServerBuilder;

import java.io.Closeable;

public interface NettyServer extends Closeable {

    static NettyServerBuilder create() {
        return new NettyServerBuilder();
    }

    void start();

    void close();

    int port();

}
