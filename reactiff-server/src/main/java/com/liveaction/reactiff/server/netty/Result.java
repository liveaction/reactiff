package com.liveaction.reactiff.server.netty;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

public abstract class Result {

    public static Result withStatus(int status, Object data) {
        return new Result() {
            @Override
            public int status() {
                return status;
            }

            @Override
            public Publisher<?> data() {
                return Mono.just(data);
            }
        };
    }

    public static Result ok(Publisher<?> publisher) {
        return new Result() {
            @Override
            public Publisher<?> data() {
                return publisher;
            }
        };
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder copy() {
        Builder builder = new Builder();
        builder.status(status());
        builder.data(data());
        headers().forEach(builder::header);
        return builder;
    }

    public static final class Builder {

        private int status = 200;
        private Publisher data;
        private final Map<String, Set<String>> httpHeaders = Maps.newHashMap();

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder data(Publisher data) {
            this.data = data;
            return this;
        }

        public Builder header(String name, String value) {
            httpHeaders.computeIfAbsent(name, s -> Sets.newHashSet()).add(value);
            return this;
        }

        public Result build() {
            return new Result() {

                @Override
                public int status() {
                    return status;
                }

                @Override
                public Publisher data() {
                    return data;
                }

                @Override
                public Headers headers() {
                    return Headers.of(httpHeaders);
                }
            };
        }

    }

    public int status() {
        return 200;
    }

    public Headers headers() {
        return Headers.empty();
    }

    public abstract Publisher<?> data();

}
