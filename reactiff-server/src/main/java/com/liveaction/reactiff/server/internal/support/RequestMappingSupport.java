package com.liveaction.reactiff.server.internal.support;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.CodecManager;
import com.liveaction.reactiff.api.server.FilterChain;
import com.liveaction.reactiff.api.server.HttpMethod;
import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.api.server.Request;
import com.liveaction.reactiff.api.server.Result;
import com.liveaction.reactiff.api.server.annotation.*;
import com.liveaction.reactiff.api.server.route.HttpRoute;
import com.liveaction.reactiff.api.server.route.Route;
import com.liveaction.reactiff.server.internal.param.ParamUtils;
import com.liveaction.reactiff.server.internal.utils.FilterUtils;
import com.liveaction.reactiff.server.internal.utils.ResultUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RequestMappingSupport implements HandlerSupportFunction<RequestMapping, HttpRoute> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestMappingSupport.class);

    private final CodecManager codecManager;
    private final Function<FilterChain, FilterChain> filterChainer;
    private final boolean writeErrorStacktrace;

    public RequestMappingSupport(CodecManager codecManager, Function<FilterChain, FilterChain> chainFunction, boolean writeErrorStacktrace) {
        this.codecManager = codecManager;
        this.filterChainer = chainFunction;
        this.writeErrorStacktrace = writeErrorStacktrace;
    }

    @Override
    public Class<RequestMapping> supports() {
        return RequestMapping.class;
    }

    @Override
    public ImmutableSet<HttpRoute> buildRoutes(RequestMapping annotation, Method method) {
        return ImmutableSet.copyOf(Stream.of(annotation.method())
                .map(httpMethod -> Route.http(annotation.rank(), httpMethod, annotation.path(), method))
                .collect(Collectors.toList()));
    }

    @Override
    public void register(HttpServerRoutes httpServerRoutes, ReactiveHandler reactiveHandler, HttpRoute route) {
        Method method = route.handlerMethod();
        FilterChain routeChain = (request) -> invokeHandlerMethod(reactiveHandler, method, request);
        BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> onRequest = (req, res) -> {
            Optional<Route> matchingRoute = Optional.of(Route.http(0, HttpMethod.valueOf(req.method().name()), route.path(), method));
            return FilterUtils.applyFilters(req, res, codecManager, filterChainer, routeChain, matchingRoute, writeErrorStacktrace);
        };
        route.method.route(httpServerRoutes, route.path(), onRequest);
        LOGGER.trace("Registered route {}", route);
    }

    private Mono<Result> invokeHandlerMethod(ReactiveHandler reactiveHandler, Method method, Request request) {
        try {
            TypeToken<?> returnType = TypeToken.of(method.getGenericReturnType());
            List<Object> args = Lists.newArrayList();
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < method.getParameterCount(); i++) {
                Parameter parameter = parameters[i];
                TypeToken<?> parameterType = TypeToken.of(parameter.getType());
                if (parameterType.isAssignableFrom(Request.class)) {
                    args.add(request);
                } else {
                    PathParam annotation;
                    HeaderParam headerAnnotation;
                    UriParam uriParam;
                    if ((annotation = parameter.getAnnotation(PathParam.class)) != null) {
                        String name = annotation.value();
                        if (name.isEmpty()) {
                            name = parameter.getName();
                        }
                        args.add(ParamUtils.convertValue(request.pathParam(name), parameterType));
                    } else if (parameter.getAnnotation(RequestBody.class) != null) {
                        TypeToken<?> parametrizedType = TypeToken.of(parameter.getParameterizedType());
                        if (parameterType.isAssignableFrom(Mono.class)) {
                            TypeToken<?> paramType = parametrizedType.resolveType(Mono.class.getTypeParameters()[0]);
                            args.add(request.bodyToMono(paramType));
                        } else if (parameterType.isAssignableFrom(Flux.class)) {
                            TypeToken<?> paramType = parametrizedType.resolveType(Flux.class.getTypeParameters()[0]);
                            args.add(request.bodyToFlux(paramType));
                        } else {
                            throw new IllegalArgumentException(RequestBody.class.getSimpleName() + " only support Mono<T> or Flux<T> type");
                        }
                    } else if ((headerAnnotation = parameter.getAnnotation(HeaderParam.class)) != null) {
                        String name = headerAnnotation.value();
                        if (name.isEmpty()) {
                            name = parameter.getName();
                        }
                        args.add(ParamUtils.convertValue(request.header(name), parameterType));
                    } else if ((uriParam = parameter.getAnnotation(UriParam.class)) != null) {
                        String name = uriParam.value();
                        if (name.isEmpty()) {
                            name = parameter.getName();
                        }
                        args.add(ParamUtils.convertValue(request.uriParam(name), parameterType));
                    }
                }
            }
            Object rawResult = method.invoke(reactiveHandler, args.toArray());
            return ResultUtils.toResult(returnType, rawResult);
        } catch (IllegalAccessException e) {
            return Mono.error(e);
        } catch (InvocationTargetException e) {
            return Mono.error(e.getTargetException());
        }
    }

}