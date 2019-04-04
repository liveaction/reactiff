package com.liveaction.reactiff.codec.jackson.model;

import java.util.Objects;

public final class MapKey {

    public final String start;
    public final String end;

    public static final MapKey fromString(String s) {
        String[] split = s.split(":");
        return new MapKey(split[0], split[1]);
    }

    private MapKey(String start, String end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MapKey mapKey = (MapKey) o;
        return Objects.equals(start, mapKey.start) &&
                Objects.equals(end, mapKey.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return start + ':' + end;
    }

}
