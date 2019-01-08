package com.liveaction.reactiff.server.netty;

public interface Rankable<T> extends Comparable<Rankable<T>> {

    int rank();

    @Override
    default int compareTo(Rankable<T> o) {
        return rank() - o.rank();
    }

}
