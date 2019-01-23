package com.liveaction.reactiff.server.netty;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;

public class Headers {

    private static Headers EMPTY = new Headers(ImmutableMap.of());

    public static Headers empty() {
        return EMPTY;
    }

    public static Headers of(Map<String, ? extends Iterable<String>> values) {
        return new Headers(values);
    }

    private final Map<String, Iterable<String>> values;

    private Headers(Map<String, ? extends Iterable<String>> values) {
        this.values = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.values.putAll(values);
    }

    public String get(String name) {
        return Iterables.getFirst(values.get(name), null);
    }

    public ImmutableList<String> getAll(String name) {
        return ImmutableList.copyOf(values.get(name));
    }

    public void forEach(BiConsumer<String, String> headerConsumer) {
        values.forEach((s, strings) -> strings.forEach(s1 -> headerConsumer.accept(s, s1)));
    }

}
