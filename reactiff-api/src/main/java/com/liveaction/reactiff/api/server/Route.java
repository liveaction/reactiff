package com.liveaction.reactiff.api.server;

import com.google.common.base.MoreObjects;

import java.lang.reflect.Method;
import java.util.Objects;

public final class Route {

    public final HttpMethod method;
    public final String path;
    public final Method handlerMethod;

    public Route(HttpMethod method, String path, Method handlerMethod) {
        this.method = method;
        this.path = path;
        this.handlerMethod = handlerMethod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Route route = (Route) o;
        return method == route.method &&
                Objects.equals(path, route.path) &&
                Objects.equals(handlerMethod, route.handlerMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, path, handlerMethod);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("method", method)
                .add("path", path)
                .add("handlerMethod", handlerMethod)
                .toString();
    }

}
