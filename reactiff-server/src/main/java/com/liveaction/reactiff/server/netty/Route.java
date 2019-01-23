package com.liveaction.reactiff.server.netty;

import com.google.common.base.MoreObjects;

import java.util.Objects;

public final class Route {

    public final HttpMethod method;
    public final String path;

    public Route(HttpMethod method, String path) {
        this.method = method;
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Route route = (Route) o;
        return method == route.method &&
                Objects.equals(path, route.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, path);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("method", method)
                .add("path", path)
                .toString();
    }

}
