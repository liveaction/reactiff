package com.liveaction.reactiff.server.internal.support;

import com.google.common.collect.ImmutableSet;
import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.api.server.annotation.WsMapping;
import com.liveaction.reactiff.api.server.route.Route;
import com.liveaction.reactiff.api.server.route.WebSocketRoute;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRoutes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class WsMappingSupport implements HandlerSupportFunction<WsMapping, WebSocketRoute> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WsMappingSupport.class);

    @Override
    public Class<WsMapping> supports() {
        return WsMapping.class;
    }

    @Override
    public ImmutableSet<WebSocketRoute> buildRoutes(WsMapping annotation, Method method) {
        return ImmutableSet.of(Route.webSocket(annotation.rank(), annotation.path(), method));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void register(HttpServerRoutes httpServerRoutes, ReactiveHandler reactiveHandler, WebSocketRoute route) {
        httpServerRoutes.ws(route.path(), (req, res) -> {
            try {
                return (Publisher<Void>) route.handlerMethod.invoke(reactiveHandler, req, res);
            } catch (IllegalAccessException | InvocationTargetException error) {
                return Mono.error(error);
            }
        });
        LOGGER.trace("Registered route {}", route);
    }

}
