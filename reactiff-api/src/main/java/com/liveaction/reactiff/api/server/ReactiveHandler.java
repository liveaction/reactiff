package com.liveaction.reactiff.api.server;

public interface ReactiveHandler extends Comparable<ReactiveHandler> {

    default int handlerRank() {
        return 0;
    }

    @Override
    default int compareTo(ReactiveHandler o) {
        int res = this.handlerRank() - o.handlerRank();
        if (res == 0) {
            res = this.getClass().getName().compareTo(o.getClass().getName());
        }
        return res;
    }

}
