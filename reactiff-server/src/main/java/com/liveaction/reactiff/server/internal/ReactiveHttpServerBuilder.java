package com.liveaction.reactiff.server.internal;

import com.google.common.collect.Sets;
import com.liveaction.reactiff.api.codec.CodecManager;
import com.liveaction.reactiff.api.server.ReactiveFilter;
import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.codec.CodecManagerImpl;
import com.liveaction.reactiff.server.ReactiveHttpServer;
import reactor.netty.http.HttpProtocol;
import reactor.util.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;

public final class ReactiveHttpServerBuilder implements ReactiveHttpServer.Builder {

    private String host = "0.0.0.0";

    private int port = -1;

    private Collection<HttpProtocol> protocols = Sets.newHashSet();

    private Collection<ReactiveFilter> filters = Sets.newHashSet();

    private Collection<ReactiveHandler> handlers = Sets.newHashSet();

    @Nullable
    private CodecManager codecManager;

    @Override
    public ReactiveHttpServer.Builder host(String host) {
        this.host = host;
        return this;
    }

    @Override
    public ReactiveHttpServer.Builder port(int port) {
        this.port = port;
        return this;
    }

    @Override
    public ReactiveHttpServer.Builder protocols(HttpProtocol... protocols) {
        this.protocols.addAll(Arrays.asList(protocols));
        return this;
    }

    @Override
    public ReactiveHttpServer.Builder filters(Iterable<ReactiveFilter> filters) {
        filters.forEach(this.filters::add);
        return this;
    }

    @Override
    public ReactiveHttpServer.Builder filter(ReactiveFilter filter) {
        filters.add(filter);
        return this;
    }

    @Override
    public ReactiveHttpServer.Builder handler(ReactiveHandler handler) {
        this.handlers.add(handler);
        return this;
    }

    @Override
    public ReactiveHttpServer.Builder codecManager(CodecManager codecManager) {
        this.codecManager = codecManager;
        return this;
    }

    @Override
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