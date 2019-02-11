package com.liveaction.reactiff.api.server;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;

public class Headers {

    private static Headers EMPTY = new Headers(ImmutableMap.of());

    public static Headers empty() {
        return EMPTY;
    }

    public static Headers of(Map<String, String> values) {
        return new Headers(values);
    }

    private final Map<String, String> values;

    private Headers(Map<String, String> values) {
        this.values = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.values.putAll(values);
    }

    public String get(String name) {
        return values.get(name);
    }

    public void forEach(BiConsumer<String, String> headerConsumer) {
        values.forEach(headerConsumer::accept);
    }

}
