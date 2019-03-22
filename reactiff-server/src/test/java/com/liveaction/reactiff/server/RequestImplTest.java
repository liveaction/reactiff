package com.liveaction.reactiff.server;

import com.google.common.collect.ImmutableList;
import com.liveaction.reactiff.server.rules.WithCodecManager;
import io.netty.handler.codec.http.HttpMethod;
import org.assertj.core.api.Assertions;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;
import reactor.netty.http.server.HttpServerRequest;

import java.util.Optional;

public final class RequestImplTest {

    @ClassRule
    public static final WithCodecManager withCodecManager = new WithCodecManager();

    @Test
    public void shouldDecodeEmptyUriParams() {
        HttpServerRequest httpServerRequest = Mockito.mock(HttpServerRequest.class);
        Mockito.when(httpServerRequest.method()).thenReturn(new HttpMethod("GET"));
        Mockito.when(httpServerRequest.uri()).thenReturn("");

        RequestImpl request = new RequestImpl(httpServerRequest, withCodecManager.codecManager, Optional.empty());
        Assertions.assertThat(request.uriParams()).isEmpty();
    }

    @Test
    public void shouldDecodeOneUriParam() {
        HttpServerRequest httpServerRequest = Mockito.mock(HttpServerRequest.class);
        Mockito.when(httpServerRequest.method()).thenReturn(new HttpMethod("GET"));
        Mockito.when(httpServerRequest.uri()).thenReturn("/test?value=1");

        RequestImpl request = new RequestImpl(httpServerRequest, withCodecManager.codecManager, Optional.empty());
        Assertions.assertThat(request.uriParams())
                .containsOnly(
                        Assertions.entry("value", ImmutableList.of("1"))
                );
    }

    @Test
    public void shouldDecodeSeveralUriParams() {
        HttpServerRequest httpServerRequest = Mockito.mock(HttpServerRequest.class);
        Mockito.when(httpServerRequest.method()).thenReturn(new HttpMethod("GET"));
        Mockito.when(httpServerRequest.uri()).thenReturn("/test?value=1&value=2&test=true");

        RequestImpl request = new RequestImpl(httpServerRequest, withCodecManager.codecManager, Optional.empty());
        Assertions.assertThat(request.uriParams())
                .containsOnly(
                        Assertions.entry("value", ImmutableList.of("1", "2")),
                        Assertions.entry("test", ImmutableList.of("true"))
                );
    }

    @Test
    public void shouldDecodeSeveralUriParamsWithNonEncodedValue() {
        HttpServerRequest httpServerRequest = Mockito.mock(HttpServerRequest.class);
        Mockito.when(httpServerRequest.method()).thenReturn(new HttpMethod("GET"));
        Mockito.when(httpServerRequest.uri()).thenReturn("/test?value=1&value=2&test=not:encoded:value");

        RequestImpl request = new RequestImpl(httpServerRequest, withCodecManager.codecManager, Optional.empty());
        Assertions.assertThat(request.uriParams())
                .containsOnly(
                        Assertions.entry("value", ImmutableList.of("1", "2")),
                        Assertions.entry("test", ImmutableList.of("not:encoded:value"))
                );
    }

    @Test
    public void shouldDecodeSeveralUriParamsWithEncodedValue() {
        HttpServerRequest httpServerRequest = Mockito.mock(HttpServerRequest.class);
        Mockito.when(httpServerRequest.method()).thenReturn(new HttpMethod("GET"));
        Mockito.when(httpServerRequest.uri()).thenReturn("/test?value=2&key1=value+1&test=value:ispartially%3Aencoded&key2=value%40%21%242&key3=value%253&value=1");

        RequestImpl request = new RequestImpl(httpServerRequest, withCodecManager.codecManager, Optional.empty());
        Assertions.assertThat(request.uriParams())
                .containsOnly(
                        Assertions.entry("value", ImmutableList.of("2", "1")),
                        Assertions.entry("test", ImmutableList.of("value:ispartially:encoded")),
                        Assertions.entry("key1", ImmutableList.of("value 1")),
                        Assertions.entry("key2", ImmutableList.of("value@!$2")),
                        Assertions.entry("key3", ImmutableList.of("value%3"))
                );
    }

}