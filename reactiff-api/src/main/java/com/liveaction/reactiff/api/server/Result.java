package com.liveaction.reactiff.api.server;

import com.google.common.collect.Maps;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.reactivestreams.Publisher;

import java.util.Collection;
import java.util.Map;

public abstract class Result {

    public static Result withStatus(int status, String reasonPhrase) {
        return new Result() {
            @Override
            public HttpResponseStatus status() {
                return HttpResponseStatus.valueOf(status, reasonPhrase);
            }

            @Override
            public Publisher<?> data() {
                return null;
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

        private HttpResponseStatus status = HttpResponseStatus.valueOf(200);
        private Publisher data;
        private final Map<String, String> httpHeaders = Maps.newHashMap();

        public Builder status(int status, String reasonPhrase) {
            this.status = HttpResponseStatus.valueOf(status, reasonPhrase);
            return this;
        }

        public Builder status(HttpResponseStatus status) {
            this.status = status;
            return this;
        }

        public Builder data(Publisher data) {
            this.data = data;
            return this;
        }

        public Builder header(String name, String value) {
            httpHeaders.put(name, value);
            return this;
        }

        public Builder headers(String name, Collection<String> values) {
            httpHeaders.put(name, String.join(",", values));
            return this;
        }

        public Result build() {
            return new Result() {

                @Override
                public HttpResponseStatus status() {
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

    public HttpResponseStatus status() {
        return HttpResponseStatus.valueOf(200);
    }

    public Headers headers() {
        return Headers.empty();
    }

    public abstract Publisher<?> data();

}
