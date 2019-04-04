package com.liveaction.reactiff.codec.jackson.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public final class Pojo {
    public final String type;
    public final String value;

    public Pojo(@JsonProperty("type") String type,
                @JsonProperty("value") String value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pojo pojo = (Pojo) o;
        return Objects.equals(type, pojo.type) &&
                Objects.equals(value, pojo.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

    @Override
    public String toString() {
        return "Pojo{" + "type='" + type + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
