package com.liveaction.reactiff.server.netty;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public interface Result<T> {

    static <A> Result<A> withCode(int code, A data) {
        return new Result<A>() {
            @Override
            public int status() {
                return code;
            }

            @Override
            public Publisher<A> data() {
                return Mono.just(data);
            }
        };
    }

    static <A> Result<A> ok(Publisher<A> publisher) {
        return new Result<A>() {
            @Override
            public int status() {
                return 200;
            }

            @Override
            public Publisher<A> data() {
                return publisher;
            }
        };
    }

    int status();

    Publisher<T> data();

}
