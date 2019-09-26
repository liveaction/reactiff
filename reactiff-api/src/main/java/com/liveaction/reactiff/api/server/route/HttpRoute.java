package com.liveaction.reactiff.api.server.route;

import com.liveaction.reactiff.api.server.HttpMethod;

import java.lang.reflect.Method;
import java.util.Objects;

public final class HttpRoute extends Route {

    public final int rank;
    public final HttpMethod method;
    public final String path;
    public final Method handlerMethod;

    HttpRoute(int rank, HttpMethod method, String path, Method handlerMethod) {
        this.rank = rank;
        this.method = method;
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
        return method.name();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpRoute httpRoute = (HttpRoute) o;
        return rank == httpRoute.rank &&
                method == httpRoute.method &&
                Objects.equals(path, httpRoute.path) &&
                Objects.equals(handlerMethod, httpRoute.handlerMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rank, method, path, handlerMethod);
    }

}
