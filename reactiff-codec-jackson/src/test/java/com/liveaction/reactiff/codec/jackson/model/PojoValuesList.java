package com.liveaction.reactiff.codec.jackson.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.MoreObjects;

import java.util.List;
import java.util.Objects;

public final class PojoValuesList {

    public final List<PojoValues> pojos;

    @JsonCreator
    public PojoValuesList(List<PojoValues> pojos) {
        this.pojos = pojos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PojoValuesList pojoValuesList = (PojoValuesList) o;
        return Objects.equals(pojos, pojoValuesList.pojos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pojos);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("pojos", pojos)
                .toString();
    }

}
