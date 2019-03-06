package com.liveaction.reactiff.server.rules;

import com.liveaction.reactiff.api.server.ReactiveFilter;
import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.server.ReactiveHttpServer;
import org.junit.rules.ExternalResource;
import reactor.netty.http.HttpProtocol;

public final class WithReactiveServer extends ExternalResource {

    public final ReactiveHttpServer server;

    public WithReactiveServer(WithCodecManager withCodecManager) {
        server = ReactiveHttpServer.create()
                .compress(true)
                .protocols(HttpProtocol.HTTP11)
                .codecManager(withCodecManager.codecManager)
                .build();
    }

    public WithReactiveServer withFilter(ReactiveFilter reactiveFilter) {
        server.addReactiveFilter(reactiveFilter);
        return this;
    }

    public WithReactiveServer withHandler(ReactiveHandler reactiveHandler) {
        server.addReactiveHandler(reactiveHandler);
        return this;
    }

    public void before() {
        server.start();
    }

    public void after() {
        server.close();
    }

}
