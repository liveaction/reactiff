package com.liveaction.reactiff.server.netty.example;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.liveaction.reactiff.server.netty.FilterChain;
import com.liveaction.reactiff.server.netty.HttpMethod;
import com.liveaction.reactiff.server.netty.ReactiveFilter;
import com.liveaction.reactiff.server.netty.Request;
import com.liveaction.reactiff.server.netty.Result;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;

import java.util.Objects;
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

        return preflight(request, chain, originHeader);
    }

    private Mono<Result> preflight(Request request, FilterChain chain, String originHeader) {

        String accessControlMethod = request.header(ACCESS_CONTROL_REQUEST_METHOD);
        if (originHeader == null || accessControlMethod == null) {
            return chain.chain(request);
        }
        Result.Builder resultBuilder = Result.builder();

        try {
            HttpMethod parsedMethod = HttpMethod.valueOf(accessControlMethod);
            if (!this.allowedMethods.contains(parsedMethod)) {
                resultBuilder.status(401, "No such method for this route");
            } else {
                emitValidCORSResponse(originHeader, resultBuilder);
            }

        } catch (IllegalArgumentException e) {
            resultBuilder.status(401, "No such method for this route");
        }

        return Mono.just(resultBuilder.build());

    }

    private void emitValidCORSResponse(String originHeader, Result.Builder resultBuilder) {
        resultBuilder.status(HttpResponseStatus.ACCEPTED);
        if (this.maxAge != null) {
            resultBuilder.header(ACCESS_CONTROL_MAX_AGE, String.valueOf(this.maxAge));
        }

        resultBuilder.headers(ACCESS_CONTROL_ALLOW_ORIGIN, getAllowedOriginsHeader(originHeader))
                .headers(ACCESS_CONTROL_ALLOW_METHODS, allowedMethods.stream().map(Objects::toString).collect(Collectors.toSet()))
                .headers(ACCESS_CONTROL_ALLOW_HEADERS, allowedHeaders);
        if (allowCredentials) {
            resultBuilder.header(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }
    }

    private Mono<Result> retrieveAndReturnResult(Request request, FilterChain chain, String originHeader) {
        Mono<Result> result = chain.chain(request);

        // Is it actually a CORS request?
        if (originHeader != null) {
            result = result.map(r -> {

                Result.Builder copy = r.copy();

                copy.headers(ACCESS_CONTROL_ALLOW_ORIGIN, getAllowedOriginsHeader(originHeader));
                if (allowCredentials) {
                    copy.header(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
                }
                if (!allowedHeaders.isEmpty()) {
                    copy.headers(ACCESS_CONTROL_EXPOSE_HEADERS, allowedHeaders);
                }
                return copy.build();
            });

        }

        return result;
    }

    private ImmutableSet<String> getAllowedOriginsHeader(String origin) {
        // If wildcard is used, only return the request supplied origin
        if (allowedOrigins.contains("*")) {
            return ImmutableSet.of(origin);
        } else {
            return ImmutableSet.copyOf(allowedOrigins);
        }
    }

}