package com.liveaction.reactiff.api.server;

import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.reactivestreams.Publisher;

import java.util.Collection;
import java.util.Map;

public abstract class Result<T> {

    public static Result withStatus(int status, String reasonPhrase) {
        return new Result<Void>() {
            @Override
            public HttpResponseStatus status() {
                return HttpResponseStatus.valueOf(status, reasonPhrase);
            }

            @Override
            public Publisher<Void> data() {
                return null;
            }

            @Override
            public TypeToken<Void> type() {
                return TypeToken.of(Void.class);
            }
        };
    }

    public static <T> Result<T> ok(Publisher<T> publisher, TypeToken<T> type) {
        return new Result<T>() {
            @Override
            public Publisher<T> data() {
                return publisher;
            }

            @Override
            public TypeToken<T> type() {
                return type;
            }
        };
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder copy() {
        Builder<T> builder = new Builder<>();
        builder.status(status());
        builder.data(data(), type());
        headers().forEach(builder::header);
        return builder;
    }

    public static final class Builder<T> {

        private HttpResponseStatus status = HttpResponseStatus.valueOf(200);
        private Publisher<T> data;
        private TypeToken<T> type;
        private final Map<String, String> httpHeaders = Maps.newHashMap();

        public Builder status(int status, String reasonPhrase) {
            this.status = HttpResponseStatus.valueOf(status, reasonPhrase);
            return this;
        }

        public Builder status(HttpResponseStatus status) {
            this.status = status;
            return this;
        }

        public Builder data(Publisher<T> data, TypeToken<T> type) {
            this.data = data;
            this.type = type;
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

        public Result<T> build() {
            return new Result<T>() {

                @Override
                public HttpResponseStatus status() {
                    return status;
                }

                @Override
                public Publisher<T> data() {
                    return data;
                }

                @Override
                public TypeToken<T> type() {
                    return type;
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

    public abstract Publisher<T> data();

    public abstract TypeToken<T> type();

}
