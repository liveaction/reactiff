package com.liveaction.reactiff.server.netty;

public interface ReactiveHandler extends Comparable<ReactiveHandler> {

    default int handlerRank() {
        return 0;
    }

    @Override
    default int compareTo(ReactiveHandler o) {
        return this.handlerRank() - o.handlerRank();
    }

}
