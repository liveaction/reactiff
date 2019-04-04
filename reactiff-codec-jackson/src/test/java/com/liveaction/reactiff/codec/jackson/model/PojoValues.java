package com.liveaction.reactiff.codec.jackson.model;

import com.google.common.base.MoreObjects;

import java.util.Objects;

public final class PojoValues {
    public final String type;
    public final String value;
    public final String value2;
    public final String value3;
    public final String value4;
    public final String value5;
    public final String value6;
    public final String value7;
    public final String value8;
    public final String value9;

    public PojoValues(String type, String value) {
        this.type = type;
        this.value = value;
        this.value2 = value;
        this.value3 = value;
        this.value4 = value;
        this.value5 = value;
        this.value6 = value;
        this.value7 = value;
        this.value8 = value;
        this.value9 = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PojoValues that = (PojoValues) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(value, that.value) &&
                Objects.equals(value2, that.value2) &&
                Objects.equals(value3, that.value3) &&
                Objects.equals(value4, that.value4) &&
                Objects.equals(value5, that.value5) &&
                Objects.equals(value6, that.value6) &&
                Objects.equals(value7, that.value7) &&
                Objects.equals(value8, that.value8) &&
                Objects.equals(value9, that.value9);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value, value2, value3, value4, value5, value6, value7, value8, value9);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("type", type)
                .add("value", value)
                .add("value2", value2)
                .add("value3", value3)
                .add("value4", value4)
                .add("value5", value5)
                .add("value6", value6)
                .add("value7", value7)
                .add("value8", value8)
                .add("value9", value9)
                .toString();
    }
}
