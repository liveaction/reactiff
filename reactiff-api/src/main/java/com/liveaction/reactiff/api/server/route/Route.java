package com.liveaction.reactiff.api.server.route;

import com.liveaction.reactiff.api.server.HttpMethod;
import com.liveaction.reactiff.api.server.utils.FormatUtils;

import java.lang.reflect.Method;

public abstract class Route {

    public static HttpRoute http(int rank, HttpMethod method, String path, Method handlerMethod) {
        return new HttpRoute(rank, method, path, handlerMethod);
    }

    public static WebSocketRoute webSocket(int rank, String path, Method handlerMethod) {
        return new WebSocketRoute(rank, path, handlerMethod);
    }

    public abstract int rank();

    public abstract String descriptor();

    public abstract String path();

    public abstract Method handlerMethod();

    @Override
    public String toString() {
        return String.format("%s %s => %s : %s", descriptor(), path(), FormatUtils.formatMethodName(handlerMethod()), FormatUtils.formatReturnType(handlerMethod().getGenericReturnType()));
    }

    Route() {
    }

}
