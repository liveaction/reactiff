package com.liveaction.reactiff.server.netty;

import com.google.common.collect.Sets;
import com.liveaction.reactiff.codec.Codec;
import com.liveaction.reactiff.codec.CodecManagerImpl;
import reactor.netty.http.HttpProtocol;

import java.util.Arrays;
import java.util.Collection;

public class NettyServerBuilder {
    private String host = "0.0.0.0";
    private int port;
    private Collection<HttpProtocol> protocols = Sets.newHashSet();
    private Collection<ReactiveFilter> filters = Sets.newHashSet();
    private Collection<ReactiveHandler> handlers = Sets.newHashSet();
    private CodecManagerImpl codecManager = new CodecManagerImpl();

    /**
     * 0.0.0.0 by default
     * @param host
     * @return
     */
    public NettyServerBuilder setHost(String host) {
        this.host = host;
        return this;
    }

    public NettyServerBuilder setPort(int port) {
        this.port = port;
        return this;
    }

    public NettyServerBuilder addProtocols(HttpProtocol... protocols) {
        this.protocols.addAll(Arrays.asList(protocols));
        return this;
    }

    public NettyServerBuilder addFilters(ReactiveFilter... filters) {
        this.filters.addAll(Arrays.asList(filters));
        return this;
    }

    public NettyServerBuilder addHandlers(ReactiveHandler handlers) {
        this.handlers.addAll(Arrays.asList(handlers));
        return this;
    }

    public NettyServerBuilder addCodec(Codec codec) {
        this.codecManager.addCodec(codec);
        return this;
    }

    public NettyServer build() {
        return new NettyServer(host, port, protocols, filters, handlers, codecManager);
    }
}