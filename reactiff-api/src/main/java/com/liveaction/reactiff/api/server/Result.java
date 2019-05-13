package com.liveaction.reactiff.api.server;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.Cookie;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

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

    public static Result internalServer(String reasonPhrase) {
        return Result.withStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), reasonPhrase);
    }

    public static Result forbidden(String reasonPhrase) {
        return Result.withStatus(HttpResponseStatus.FORBIDDEN.code(), reasonPhrase);
    }

    public static Result badRequest(String reasonPhrase) {
        return Result.withStatus(HttpResponseStatus.BAD_REQUEST.code(), reasonPhrase);
    }

    public static Result noContent(String reasonPhrase) {
        return Result.withStatus(HttpResponseStatus.NO_CONTENT.code(), reasonPhrase);
    }

    public static Result notFound(String reasonPhrase) {
        return Result.withStatus(HttpResponseStatus.NOT_FOUND.code(), reasonPhrase);
    }

    public static Result redirectTemporary(String url) {
        return Result.builder()
                .status(HttpResponseStatus.TEMPORARY_REDIRECT)
                .header(HttpHeaderNames.LOCATION, url)
                .build();
    }

    public static Result redirect(String url) {
        return Result.builder()
                .status(HttpResponseStatus.SEE_OTHER)
                .header(HttpHeaderNames.LOCATION, url)
                .build();
    }

    public Builder<T> copy() {
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
        private final Set<Cookie> httpCookies = Sets.newHashSet();


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

        public Builder<BT> cookie(Cookie c) {
            this.httpCookies.add(c);
            return this;
        }

        public Builder<BT> cookies(Cookie... cookies) {
            this.httpCookies.addAll(Arrays.asList(cookies));
            return this;
        }

        public Builder<BT> header(CharSequence name, String value) {
            return header(name, value, false);
        }

        public Builder<BT> header(CharSequence name, String value, boolean override) {
            if (override) {
                this.httpHeaders.put(name.toString(), value);
            } else {
                this.httpHeaders.putIfAbsent(name.toString(), value);
            }
            return this;
        }

        public Builder<BT> headers(CharSequence name, Collection<String> values) {
            this.httpHeaders.put(name.toString(), String.join(",", values));
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

                @Override
                public Set<Cookie> cookies() {
                    return ImmutableSet.copyOf(httpCookies);
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

    public Set<Cookie> cookies() {
        return ImmutableSet.of();
    }

    public abstract Publisher<T> data();

    public abstract TypeToken<T> type();

}
