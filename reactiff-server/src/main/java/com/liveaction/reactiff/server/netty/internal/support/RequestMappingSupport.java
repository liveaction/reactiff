package com.liveaction.reactiff.server.netty.internal.support;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.codec.CodecManager;
import com.liveaction.reactiff.server.netty.FilterChain;
import com.liveaction.reactiff.server.netty.HttpMethod;
import com.liveaction.reactiff.server.netty.ReactiveFilter;
import com.liveaction.reactiff.server.netty.ReactiveHandler;
import com.liveaction.reactiff.server.netty.Request;
import com.liveaction.reactiff.server.netty.Route;
import com.liveaction.reactiff.server.netty.annotation.RequestMapping;
import com.liveaction.reactiff.server.netty.internal.FilterUtils;
import com.liveaction.reactiff.server.netty.internal.ResultUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

public class RequestMappingSupport implements HandlerSupportFunction<RequestMapping> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestMappingSupport.class);

    private final CodecManager codecManager;
    private final Set<ReactiveFilter> reactiveFilters;

    public RequestMappingSupport(CodecManager codecManager, Set<ReactiveFilter> reactiveFilters) {
        this.codecManager = codecManager;
        this.reactiveFilters = reactiveFilters;
    }

    @Override
    public Class<RequestMapping> supports() {
        return RequestMapping.class;
    }

    @Override
    public int rank(RequestMapping annotation) {
        return annotation.rank();
    }

    @Override
    public void register(HttpServerRoutes httpServerRoutes, RequestMapping annotation, ReactiveHandler reactiveHandler, Method method) {
        FilterChain routeChain = (request) -> {
            try {
                TypeToken<?> returnType = TypeToken.of(method.getGenericReturnType());

                List<Object> args = Lists.newArrayList();
                for (int i = 0; i < method.getParameterCount(); i++) {
                    Type[] genericParameterTypes = method.getGenericParameterTypes();
                    TypeToken<?> genericParameterType = TypeToken.of(genericParameterTypes[i]);
                    if (genericParameterType.isAssignableFrom(Request.class)) {
                        args.add(request);
                    }
                }
                Object rawResult = method.invoke(reactiveHandler, args.toArray());
                return ResultUtils.toResult(returnType, rawResult);
            } catch (IllegalAccessException | InvocationTargetException error) {
                return Mono.error(error);
            }
        };
        BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> onRequest = (req, res) -> {
            Optional<Route> matchingRoute = Optional.of(new Route(HttpMethod.valueOf(req.method().name()), annotation.path(), method));
            return FilterUtils.applyFilters(req, res, codecManager, reactiveFilters, routeChain, matchingRoute);
        };
        for (HttpMethod httpMethod : annotation.method()) {
            httpMethod.route(httpServerRoutes, annotation.path(), onRequest);
        }
        LOGGER.info("Registered route {} : '{}' -> {}", annotation.method(), annotation.path(), method);
    }

}
