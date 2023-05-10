package com.liveaction.reactiff.api.server;

import reactor.core.publisher.Mono;

public interface ReactiveFilter extends Comparable<ReactiveFilter> {

    /**
     * Rank of the filter. Lower ranks get executed first
     */
    default int filterRank() {
        return 0;
    }

    @Override
    default int compareTo(ReactiveFilter o) {
        int res = this.filterRank() - o.filterRank();
        if (res == 0) {
            res = this.getClass().getName().compareTo(o.getClass().getName());
        }
        return res;
    }

    Mono<Result> filter(Request request, FilterChain chain);

}
