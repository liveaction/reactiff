package com.liveaction.reactiff.server.netty.internal;

import com.google.common.collect.Sets;
import com.liveaction.reactiff.codec.CodecManager;
import com.liveaction.reactiff.codec.CodecManagerImpl;
import com.liveaction.reactiff.server.netty.ReactiveFilter;
import com.liveaction.reactiff.server.netty.ReactiveHandler;
import reactor.netty.http.HttpProtocol;
import reactor.util.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;

public class ReactiveHttpServerBuilder {

    private String host = "0.0.0.0";

    private int port = -1;

    private Collection<HttpProtocol> protocols = Sets.newHashSet();

    private Collection<ReactiveFilter> filters = Sets.newHashSet();

    private Collection<ReactiveHandler> handlers = Sets.newHashSet();

    @Nullable
    private CodecManager codecManager;

    /**
     * 0.0.0.0 by default
     */
    public ReactiveHttpServerBuilder host(String host) {
        this.host = host;
        return this;
    }

    /**
     * -1 by default which means random.
     */
    public ReactiveHttpServerBuilder port(int port) {
        this.port = port;
        return this;
    }

    public ReactiveHttpServerBuilder protocols(HttpProtocol... protocols) {
        this.protocols.addAll(Arrays.asList(protocols));
        return this;
    }

    public ReactiveHttpServerBuilder filters(Iterable<ReactiveFilter> filters) {
        filters.forEach(this.filters::add);
        return this;
    }

    public ReactiveHttpServerBuilder filter(ReactiveFilter filter) {
        filters.add(filter);
        return this;
    }

    public ReactiveHttpServerBuilder handler(ReactiveHandler handler) {
        this.handlers.add(handler);
        return this;
    }

    public ReactiveHttpServerBuilder codecManager(CodecManager codecManager) {
        this.codecManager = codecManager;
        return this;
    }

    public ReactiveHttpServerImpl build() {
        if (codecManager == null) {
            codecManager = new CodecManagerImpl();
        }
        ReactiveHttpServerImpl reactiveHttpServer = new ReactiveHttpServerImpl(host, port, protocols, codecManager);
        filters.forEach(reactiveHttpServer::addReactiveFilter);
        handlers.forEach(reactiveHttpServer::addReactiveHandler);
        return reactiveHttpServer;
    }

}