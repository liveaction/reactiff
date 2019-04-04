package com.liveaction.reactiff.server.mock;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class PojoWithValueOf {
    public final String valueOf;
    public final String id;

    public PojoWithValueOf(@JsonProperty("id") String id,
                              @JsonProperty("valueOf") String valueOf) {
        this.id = id;
        this.valueOf = valueOf;
    }

    public static PojoWithValueOf valueOf(String valueOf) {
        return new PojoWithValueOf("default", valueOf);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PojoWithValueOf that = (PojoWithValueOf) o;
        return Objects.equal(valueOf, that.valueOf) &&
                Objects.equal(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(valueOf, id);
    }
}
