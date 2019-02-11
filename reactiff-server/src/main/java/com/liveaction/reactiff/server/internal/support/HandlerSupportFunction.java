package com.liveaction.reactiff.server.internal.support;

import com.liveaction.reactiff.api.server.ReactiveHandler;
import reactor.netty.http.server.HttpServerRoutes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public interface HandlerSupportFunction<T extends Annotation> {

    Class<T> supports();

    int rank(T annotation);

    void register(HttpServerRoutes httpServerRoutes, T annotation, ReactiveHandler reactiveHandler, Method method);

}
