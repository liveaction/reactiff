package com.liveaction.reactiff.api.server.template;

import com.google.common.collect.ImmutableMap;
import reactor.core.publisher.Mono;

public interface TemplateEngine {

    Mono<String> process(String file, ImmutableMap<String, String> map);

}
