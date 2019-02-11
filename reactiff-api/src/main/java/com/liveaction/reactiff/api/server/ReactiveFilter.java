package com.liveaction.reactiff.api.server;

import reactor.core.publisher.Mono;

public interface ReactiveFilter extends Comparable<ReactiveFilter> {

    default int filterRank() {
        return 0;
    }

    @Override
    default int compareTo(ReactiveFilter o) {
        return filterRank() - o.filterRank();
    }

    Mono<Result> filter(Request request, FilterChain chain);

}
