package com.liveaction.reactiff.server.internal.support;

import com.google.common.collect.ImmutableSet;
import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.api.server.route.Route;
import reactor.netty.http.server.HttpServerRoutes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public interface HandlerSupportFunction<T extends Annotation, R extends Route> {

    Class<T> supports();

    ImmutableSet<R> buildRoutes(T annotation, Method method);

    void register(HttpServerRoutes httpServerRoutes, ReactiveHandler reactiveHandler, R route);

}
