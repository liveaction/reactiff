package com.liveaction.reactiff.server.internal.template;

import com.google.common.collect.ImmutableMap;
import com.liveaction.reactiff.api.server.Request;
import org.thymeleaf.context.IContext;

import java.util.Locale;
import java.util.Set;

public final class TemplateContext implements IContext {

    private static final String REQUEST = "request";

    private final Request request;
    private final ImmutableMap<String, Object> map;

    public TemplateContext(Request request, ImmutableMap<String, Object> map) {
        this.request = request;
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
        builder.put(REQUEST, request);
        builder.putAll(map);
        this.map = builder.build();
    }

    @Override
    public Locale getLocale() {
        return request.getLocale();
    }

    @Override
    public boolean containsVariable(String s) {
        return !map.isEmpty();
    }

    @Override
    public Set<String> getVariableNames() {
        return map.keySet();
    }

    @Override
    public Object getVariable(String s) {
        return map.get(s);
    }

}