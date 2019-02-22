package com.liveaction.reactiff.api.server.template;

import com.google.common.collect.ImmutableMap;
import com.liveaction.reactiff.api.server.Request;
import reactor.core.publisher.Mono;

public interface TemplateEngine {

    Mono<String> process(String file, String contentType, Request request, ImmutableMap<String, Object> map);

}
