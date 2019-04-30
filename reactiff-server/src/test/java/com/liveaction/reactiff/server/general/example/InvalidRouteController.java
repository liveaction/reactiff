package com.liveaction.reactiff.server.general.example;

import com.liveaction.reactiff.api.server.HttpMethod;
import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.api.server.Result;
import com.liveaction.reactiff.api.server.annotation.PathParam;
import com.liveaction.reactiff.api.server.annotation.RequestMapping;
import reactor.core.publisher.Mono;

public final class InvalidRouteController implements ReactiveHandler {


    @RequestMapping(method = HttpMethod.GET, path = "/route/valid/{param}")
    public Result<Boolean> validRoute(@PathParam String name) {
        return Result.ok(Mono.just(true), Boolean.class);
    }

    @RequestMapping(method = HttpMethod.GET, path = "/route/invalid/{wrong_param}") // param name cannot contain an '_'
    public Result<Boolean> invalidRoute() {
        return Result.ok(Mono.just(true), Boolean.class);
    }
}
