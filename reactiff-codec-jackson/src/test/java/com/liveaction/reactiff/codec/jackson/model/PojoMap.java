package com.liveaction.reactiff.codec.jackson.model;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Objects;

public final class PojoMap {

    public final ImmutableMap<String, ImmutableMap<String, Pojo>> mapOfMap;

    public PojoMap(@JsonProperty("mapOfMap") ImmutableMap<String, ImmutableMap<String, Pojo>> mapOfMap) {
        this.mapOfMap = mapOfMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PojoMap that = (PojoMap) o;
        return Objects.equals(mapOfMap, that.mapOfMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapOfMap);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("mapOfMap", mapOfMap)
                .toString();
    }

}
