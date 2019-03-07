package com.liveaction.reactiff.server.mock;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class PojoWithFrom {
    public final String from;
    public final String id;

    public PojoWithFrom(@JsonProperty("id") String id,
                        @JsonProperty("from") String from) {
        this.id = id;
        this.from = from;
    }

    public static PojoWithFrom from(String from) {
        return new PojoWithFrom("default", from);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PojoWithFrom that = (PojoWithFrom) o;
        return Objects.equal(from, that.from) &&
                Objects.equal(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(from, id);
    }
}
