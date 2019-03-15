package com.liveaction.reactiff.server;

import com.google.common.collect.ImmutableSet;
import com.liveaction.reactiff.api.server.ReactiveFilter;
import com.liveaction.reactiff.server.internal.filters.CorsFilter;
import com.liveaction.reactiff.server.internal.filters.ExceptionMappingFilter;

import java.util.function.Function;

public final class DefaultFilters {

    private DefaultFilters() {
    }

    public static ReactiveFilter cors(ImmutableSet<String> allowedOrigins,
                                      ImmutableSet<String> allowedHeaders,
                                      ImmutableSet<String> allowedMethods,
                                      boolean allowCredentials,
                                      int maxAge) {
        return new CorsFilter(allowedOrigins, allowedHeaders, allowedMethods, allowCredentials, maxAge);

    }

    public static ReactiveFilter exceptionMapping(Function<Throwable, Integer> mapping) {
        return new ExceptionMappingFilter(mapping);
    }

}
