package com.liveaction.reactiff.server.mock;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class PojoWithConstructor {
    public final String value;

    public PojoWithConstructor(@JsonProperty("value") String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PojoWithConstructor that = (PojoWithConstructor) o;
        return Objects.equal(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
