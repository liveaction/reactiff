package com.liveaction.reactiff.server.internal.template;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.NoSuchElementException;

public final class TemplateEngineImpl implements com.liveaction.reactiff.api.server.template.TemplateEngine {

    @Override
    public Mono<String> process(String file, ImmutableMap<String, String> map) {
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
                    .map(template -> {
                        for (Map.Entry<String, String> entry : map.entrySet()) {
                            template = template.replaceAll("\\{\\s*\\{\\s*" + entry.getKey() + "\\s*}\\s*}", entry.getValue());
                        }
                        return template;
                    })
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
