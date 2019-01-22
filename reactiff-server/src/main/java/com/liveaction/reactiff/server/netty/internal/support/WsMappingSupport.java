package com.liveaction.reactiff.server.netty.internal.support;

import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.codec.CodecManager;
import com.liveaction.reactiff.server.netty.ReactiveHandler;
import com.liveaction.reactiff.server.netty.annotation.WsMapping;
import com.liveaction.reactiff.server.netty.internal.ResultUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRoutes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class WsMappingSupport implements HandlerSupportFunction<WsMapping> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WsMappingSupport.class);

    private final CodecManager codecManager;

    public WsMappingSupport(CodecManager codecManager) {
        this.codecManager = codecManager;
    }

    @Override
    public Class<WsMapping> supports() {
        return WsMapping.class;
    }

    @Override
    public int rank(WsMapping annotation) {
        return annotation.rank();
    }

    @Override
    public void register(HttpServerRoutes httpServerRoutes, WsMapping annotation, ReactiveHandler reactiveHandler, Method method) {
        httpServerRoutes.ws(annotation.path(), (req, res) -> {
            try {
                TypeToken<?> returnType = TypeToken.of(method.getGenericReturnType());
                Object rawResult = method.invoke(reactiveHandler, req);
                return ResultUtils.toResult(returnType, rawResult)
                        .flatMap(result -> Mono.from(res.send(codecManager.encode(req.headers(), result.data()))));
            } catch (IllegalAccessException | InvocationTargetException error) {
                return Mono.error(error);
            }
        });
        LOGGER.info("Registered websocket '{}' -> {}", annotation.path(), method);
    }

}
