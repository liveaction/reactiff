package com.liveaction.reactiff.server.netty.internal;

import com.google.common.collect.Sets;
import com.liveaction.reactiff.codec.CodecManager;
import com.liveaction.reactiff.codec.CodecManagerImpl;
import reactor.netty.http.HttpProtocol;
import reactor.util.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;

public class ReactiveHttpServerBuilder {

    private String host = "0.0.0.0";

    private int port = -1;

    private Collection<HttpProtocol> protocols = Sets.newHashSet();

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

    public ReactiveHttpServerBuilder codecManager(CodecManager codecManager) {
        this.codecManager = codecManager;
        return this;
    }

    public ReactiveHttpServerImpl build() {
        if (codecManager == null) {
            codecManager = new CodecManagerImpl();
        }
        return new ReactiveHttpServerImpl(host, port, protocols, codecManager);
    }

}