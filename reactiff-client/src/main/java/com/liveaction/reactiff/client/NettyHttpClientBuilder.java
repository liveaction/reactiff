package com.liveaction.reactiff.client;

import com.liveaction.reactiff.codec.Codec;
import com.liveaction.reactiff.codec.CodecManager;

public class NettyHttpClientBuilder {
    private final String protocol;
    private final String host;
    private final int port;
    private CodecManager codecManager = new CodecManager();

    public NettyHttpClientBuilder(String protocol, String host, int port) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
    }


    public NettyHttpClientBuilder addCodec(Codec codec) {
        this.codecManager.addCodec(codec);
        return this;
    }

    public NettyHttpClient build() {
        return new NettyHttpClient(protocol, host, port, codecManager);
    }
}