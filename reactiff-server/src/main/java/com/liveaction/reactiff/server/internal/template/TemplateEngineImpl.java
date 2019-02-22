package com.liveaction.reactiff.server.internal.template;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import com.liveaction.reactiff.api.server.Request;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.TemplateSpec;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;

public final class TemplateEngineImpl implements com.liveaction.reactiff.api.server.template.TemplateEngine {

    private static final TemplateEngine TEMPLATE_ENGINE = new TemplateEngine();

    @Override
    public Mono<String> process(String file, String contentType, Request request, ImmutableMap<String, Object> map) {
        InputStream resource = getClass().getResourceAsStream(file);
        if (resource != null) {
            return Mono.just(resource)
                    .map(inputStream -> {
                        try {
                            return CharStreams.toString(new InputStreamReader(resource));
                        } catch (IOException e) {
                            throw Throwables.propagate(e);
                        }
                    })
                    .map(template -> TEMPLATE_ENGINE.process(new TemplateSpec(template, contentType), new TemplateContext(request, map)))
                    .doFinally(t -> {
                        try {
                            resource.close();
                        } catch (IOException ignored) {
                        }
                    });
        } else {
            return Mono.error(new NoSuchElementException(file));
        }
    }


}
