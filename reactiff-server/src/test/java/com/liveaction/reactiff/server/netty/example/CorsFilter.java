package com.liveaction.reactiff.server.netty.example;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.liveaction.reactiff.server.netty.FilterChain;
import com.liveaction.reactiff.server.netty.HttpMethod;
import com.liveaction.reactiff.server.netty.ReactiveFilter;
import com.liveaction.reactiff.server.netty.Request;
import com.liveaction.reactiff.server.netty.Result;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class CorsFilter implements ReactiveFilter {

    private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    private static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    private static final String ORIGIN = "Origin";
    private static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
    private static final ImmutableSet<String> SIMPLE_HEADERS = ImmutableSet.of("Accept", "Accept-Language", "Content-Language", "Content-Type");

    private Set<String> allowedOrigins;
    private Set<String> allowedHeaders;
    private ImmutableSet<HttpMethod> allowedMethods;
    private boolean allowCredentials;
    private Integer maxAge;

    public CorsFilter(ImmutableSet<String> allowedOrigins,
                      ImmutableSet<String> allowedHeaders,
                      ImmutableSet<String> allowedMethods,
                      boolean allowCredentials,
                      Optional<Integer> maxAge) {
        this.allowedOrigins = Sets.newTreeSet(String.CASE_INSENSITIVE_ORDER);
        this.allowedOrigins.addAll(allowedOrigins);

        this.allowedHeaders = Sets.newTreeSet(String.CASE_INSENSITIVE_ORDER);
        this.allowedHeaders.addAll(SIMPLE_HEADERS);
        this.allowedHeaders.addAll(allowedHeaders);

        this.allowedMethods = ImmutableSet.copyOf(allowedMethods.stream().map(String::toUpperCase).map(HttpMethod::valueOf).collect(Collectors.toSet()));
        this.allowCredentials = allowCredentials;
        this.maxAge = maxAge.orElse(null);
    }

    @Override
    public int filterRank() {
        return -10;
    }

    @Override
    public Mono<Result> filter(Request request, FilterChain chain) {
        // Is CORS required?
        String originHeader = request.header(ORIGIN);

        // If not Preflight
        if (request.method() != HttpMethod.OPTIONS) {
            return retrieveAndReturnResult(request, chain, originHeader);
        }

        // Try "Preflight"
        return preflight(request, chain, originHeader);
    }

    private Mono<Result> preflight(Request request, FilterChain chain, String originHeader) {

        String accessControlMethod = request.header(ACCESS_CONTROL_REQUEST_METHOD);
        if (originHeader == null || accessControlMethod == null) {
            return chain.chain(request);
        }

        Result.Builder resultBuilder = Result.builder();

        if (!this.allowedMethods.contains(request.method())) {
            resultBuilder.status(401).data(Mono.just("No such method for this route"));
        }

        if (this.maxAge != null) {
            resultBuilder.header(ACCESS_CONTROL_MAX_AGE, String.valueOf(this.maxAge));
        }

        // Otherwise we should be return OK withHeader the appropriate headers.

        String exposedHeadersHeader = getExposedHeadersHeader();
        String allowedOriginsHeader = getAllowedOriginsHeader(originHeader);

        String allowedMethodsHeader = Joiner.on(", ").join(allowedMethods);

        resultBuilder.header(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOriginsHeader)
                .header(ACCESS_CONTROL_ALLOW_METHODS, allowedMethodsHeader)
                .header(ACCESS_CONTROL_ALLOW_HEADERS, exposedHeadersHeader);
        if (allowCredentials) {
            resultBuilder.header(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }

        return Mono.just(resultBuilder.build());
    }

    private String getExposedHeadersHeader() {
        return String.join(", ", allowedHeaders);
    }

    private Mono<Result> retrieveAndReturnResult(Request request, FilterChain chain, String originHeader) {
        Mono<Result> result = chain.chain(request);

        // Is it actually a CORS request?
        if (originHeader != null) {
            result = result.map(r -> {

                Result.Builder copy = r.copy();

                String allowedOriginsHeader = getAllowedOriginsHeader(originHeader);
                copy.header(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOriginsHeader);
                if (allowCredentials) {
                    copy.header(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
                }
                if (!allowedHeaders.isEmpty()) {
                    copy.header(ACCESS_CONTROL_EXPOSE_HEADERS, getExposedHeadersHeader());
                }
                return copy.build();
            });

        }

        return result;
    }

    private String getAllowedOriginsHeader(String origin) {
        // If wildcard is used, only return the request supplied origin
        if (allowedOrigins.contains("*")) {
            return origin;
        } else {
            return Joiner.on(", ").join(allowedOrigins);
        }
    }

}