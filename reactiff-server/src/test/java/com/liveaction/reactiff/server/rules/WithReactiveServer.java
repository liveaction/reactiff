package com.liveaction.reactiff.server.rules;

import com.liveaction.reactiff.api.server.ReactiveFilter;
import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.server.ReactiveHttpServer;
import com.liveaction.reactiff.server.context.ExecutionContextService;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.junit.rules.ExternalResource;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;

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

    public WithReactiveServer withExecutionContetService(ExecutionContextService executionContetService) {
        server.addExecutionContextService(executionContetService);
        return this;
    }

    public WithReactiveServer removeHandler(ReactiveHandler reactiveHandler) {
        server.removeReactiveHandler(reactiveHandler);
        return this;
    }

    public void before() {
        server.start();
    }

    public void after() {
        server.close();
    }

    public HttpClient httpClient() {
        return HttpClient.create()
                .protocol(HttpProtocol.HTTP11)
                .baseUrl("http://localhost:" + server.port())
                .headers(httpHeaders -> httpHeaders.set(HttpHeaderNames.ACCEPT, "application/json"));
    }

}
