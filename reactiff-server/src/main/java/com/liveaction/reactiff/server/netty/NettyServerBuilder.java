package com.liveaction.reactiff.server.netty;

import com.google.common.collect.Sets;
import com.liveaction.reactiff.codec.CodecManager;
import com.liveaction.reactiff.codec.CodecManagerImpl;
import reactor.netty.http.HttpProtocol;
import reactor.util.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;

public class NettyServerBuilder {

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
    public NettyServerBuilder host(String host) {
        this.host = host;
        return this;
    }

    /**
     * -1 by default which means random.
     */
    public NettyServerBuilder port(int port) {
        this.port = port;
        return this;
    }

    public NettyServerBuilder protocols(HttpProtocol... protocols) {
        this.protocols.addAll(Arrays.asList(protocols));
        return this;
    }

    public NettyServerBuilder filters(Iterable<ReactiveFilter> filters) {
        filters.forEach(this.filters::add);
        return this;
    }

    public NettyServerBuilder filter(ReactiveFilter filter) {
        filters.add(filter);
        return this;
    }

    public NettyServerBuilder handler(ReactiveHandler handler) {
        this.handlers.add(handler);
        return this;
    }

    public NettyServerBuilder codecManager(CodecManager codecManager) {
        this.codecManager = codecManager;
        return this;
    }

    public NettyServerImpl build() {
        if (codecManager == null) {
            codecManager = new CodecManagerImpl();
        }
        return new NettyServerImpl(host, port, protocols, filters, handlers, codecManager);
    }

}