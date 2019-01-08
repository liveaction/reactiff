package com.liveaction.reactiff.server.netty;

public interface ReactiveHandler extends Rankable<ReactiveHandler> {

    @Override
    default int rank() {
        return 0;
    }

}
