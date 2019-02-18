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

    public static <T> Result<T> ok(Publisher<T> publisher, Class<T> clazz) {
        return ok(publisher, TypeToken.of(clazz));
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

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public Builder copy() {
        Builder<T> builder = new Builder<>();
        builder.status(status());
        builder.data(data(), type());
        headers().forEach(builder::header);
        return builder;
    }

    public static final class Builder<BT> {

        private HttpResponseStatus status = HttpResponseStatus.valueOf(200);
        private Publisher<BT> data;
        private TypeToken<BT> type;
        private final Map<String, String> httpHeaders = Maps.newHashMap();

        public Builder<BT> status(int status, String reasonPhrase) {
            this.status = HttpResponseStatus.valueOf(status, reasonPhrase);
            return this;
        }

        public Builder<BT> status(HttpResponseStatus status) {
            this.status = status;
            return this;
        }

        public Builder<BT> data(Publisher<BT> data, Class<BT> clazz) {
            return data(data, TypeToken.of(clazz));
        }

        public Builder<BT> data(Publisher<BT> data, TypeToken<BT> type) {
            this.data = data;
            this.type = type;
            return this;
        }

        public Builder<BT> header(String name, String value) {
            httpHeaders.put(name, value);
            return this;
        }

        public Builder<BT> headers(String name, Collection<String> values) {
            httpHeaders.put(name, String.join(",", values));
            return this;
        }

        public Result<BT> build() {
            return new Result<BT>() {

                @Override
                public HttpResponseStatus status() {
                    return status;
                }

                @Override
                public Publisher<BT> data() {
                    return data;
                }

                @Override
                public TypeToken<BT> type() {
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
