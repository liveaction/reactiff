package com.liveaction.reactiff.server.internal.support;

import com.google.common.collect.ImmutableSet;
import com.liveaction.reactiff.api.codec.CodecManager;
import com.liveaction.reactiff.api.server.FilterChain;
import com.liveaction.reactiff.api.server.HttpMethod;
import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.api.server.Result;
import com.liveaction.reactiff.api.server.annotation.WsMapping;
import com.liveaction.reactiff.api.server.route.Route;
import com.liveaction.reactiff.api.server.route.WebSocketRoute;
import com.liveaction.reactiff.server.internal.utils.FilterUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.netty.http.server.HttpServerRoutes;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Function;

public class WsMappingSupport implements HandlerSupportFunction<WsMapping, WebSocketRoute> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WsMappingSupport.class);

    private final Function<FilterChain, FilterChain> filterChainer;
    private final CodecManager codecManager;
    private final Scheduler workScheduler;

    public WsMappingSupport(Function<FilterChain, FilterChain> filterChainer, CodecManager codecManager, Scheduler workScheduler) {
        this.filterChainer = filterChainer;
        this.codecManager = codecManager;
        this.workScheduler = workScheduler;
    }

    @Override
    public Class<WsMapping> supports() {
        return WsMapping.class;
    }

    @Override
    public ImmutableSet<WebSocketRoute> buildRoutes(WsMapping annotation, Method method) {
        return ImmutableSet.of(Route.webSocket(annotation.rank(), annotation.path(), method));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void register(HttpServerRoutes httpServerRoutes, ReactiveHandler reactiveHandler, WebSocketRoute route) {
        httpServerRoutes.get(route.path, (req, res) -> {
            FilterChain chain = workScheduler == null ?
                    (httpRequest) -> Mono.from(res.sendWebsocket((wsIn, wsOut) -> executeMethod(reactiveHandler, route, wsIn, wsOut)))
                            .then(Mono.fromCallable(() -> Result.ok(Mono.empty(), Void.class)))
                    : (httpRequest) -> Mono.from(res.sendWebsocket((wsIn, wsOut) -> executeMethod(reactiveHandler, route, wsIn, wsOut)))
                    .subscribeOn(workScheduler)
                    .then(Mono.fromCallable(() -> Result.ok(Mono.empty(), Void.class)));

            Optional<Route> matchingRoute = Optional.of(Route.http(0, HttpMethod.GET, route.path(), route.handlerMethod));
            return FilterUtils.applyFilters(req, res, codecManager, filterChainer, chain, matchingRoute, false);
        });
        LOGGER.trace("Registered route {}", route);
    }

    private Publisher<Void> executeMethod(ReactiveHandler reactiveHandler, WebSocketRoute route, WebsocketInbound wsIn, WebsocketOutbound wsOut) {
        Publisher<Void> result;
        try {
            result = (Publisher<Void>) route.handlerMethod.invoke(reactiveHandler, wsIn, wsOut);
        } catch (IllegalAccessException e) {
            result = Mono.error(e);
        } catch (InvocationTargetException e) {
            result = Mono.error(e.getTargetException());
        }
        return result;
    }

}
