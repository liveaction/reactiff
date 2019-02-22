package com.liveaction.reactiff.api.server.route;

import java.lang.reflect.Method;
import java.util.Objects;

public final class WebSocketRoute extends Route {

    private static final String WEB_SOCKET = "WS";

    public final int rank;
    public final String path;
    public final Method handlerMethod;

    WebSocketRoute(int rank, String path, Method handlerMethod) {
        this.rank = rank;
        this.path = path;
        this.handlerMethod = handlerMethod;
    }

    @Override
    public int rank() {
        return rank;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public Method handlerMethod() {
        return handlerMethod;
    }

    @Override
    public String descriptor() {
        return WEB_SOCKET;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebSocketRoute that = (WebSocketRoute) o;
        return rank == that.rank &&
                Objects.equals(path, that.path) &&
                Objects.equals(handlerMethod, that.handlerMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rank, path, handlerMethod);
    }

}
