package com.liveaction.reactiff.api.server;

import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;

public abstract class Result<T> {

    public static Result<String> withStatus(int status, String reasonPhrase) {
        return Result.<String>builder()
                .status(status, reasonPhrase)
                .build();
    }

    public static Result<String> withStatus(int status, String reasonPhrase, String message) {
        return Result.<String>builder()
                .status(status, reasonPhrase)
                .data(Mono.just(message), String.class)
                .header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN.toString())
                .build();
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

    public static Result unauthorized(String reasonPhrase) {
        return Result.withStatus(HttpResponseStatus.UNAUTHORIZED.code(), reasonPhrase);
    }

    public static Result forbidden(String reasonPhrase) {
        return Result.withStatus(HttpResponseStatus.FORBIDDEN.code(), reasonPhrase);
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

        public Builder<BT> header(CharSequence name, String value) {
            httpHeaders.put(name.toString(), value);
            return this;
        }

        public Builder<BT> headers(CharSequence name, Collection<String> values) {
            httpHeaders.put(name.toString(), String.join(",", values));
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
