package com.liveaction.reactiff.server.mock;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class PojoWithFromString {
    public final String id;
    public final String fromString;

    public PojoWithFromString(@JsonProperty("id") String id,
                @JsonProperty("fromString") String fromString) {
        this.id = id;
        this.fromString = fromString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PojoWithFromString that = (PojoWithFromString) o;
        return Objects.equal(id, that.id) &&
                Objects.equal(fromString, that.fromString);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, fromString);
    }

    public static PojoWithFromString fromString(String fromString) {
        return new PojoWithFromString("default", fromString);
    }
}
