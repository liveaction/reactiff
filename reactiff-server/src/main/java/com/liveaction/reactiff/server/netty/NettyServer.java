package com.liveaction.reactiff.server.netty;

import java.io.Closeable;

public interface NettyServer extends Closeable {

    static NettyServerBuilder create() {
        return new NettyServerBuilder();
    }

    void start();

    void close();

    int port();

}
