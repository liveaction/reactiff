package com.liveaction.reactiff.server.internal.support;

import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.api.server.annotation.WsMapping;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRoutes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class WsMappingSupport implements HandlerSupportFunction<WsMapping> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WsMappingSupport.class);

    @Override
    public Class<WsMapping> supports() {
        return WsMapping.class;
    }

    @Override
    public int rank(WsMapping annotation) {
        return annotation.rank();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void register(HttpServerRoutes httpServerRoutes, WsMapping annotation, ReactiveHandler reactiveHandler, Method method) {
        httpServerRoutes.ws(annotation.path(), (req, res) -> {
            try {
                return (Publisher<Void>) method.invoke(reactiveHandler, req, res);
            } catch (IllegalAccessException | InvocationTargetException error) {
                return Mono.error(error);
            }
        });
        LOGGER.trace("Registered websocket '{}' -> {}", annotation.path(), method);
    }

}
