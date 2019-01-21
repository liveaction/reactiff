package com.liveaction.reactiff.client;

import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.codec.CodecManagerImpl;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;

public final class NettyHttpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyHttpClient.class);

    private final HttpClient httpClient;
    private final CodecManagerImpl codecManager;

    public NettyHttpClient(String protocol, String host, int port, CodecManagerImpl codecManager) {
        this.codecManager = codecManager;
        String url = protocol + "://" + host + ":" + port;
        this.httpClient = HttpClient.create()
                .protocol(HttpProtocol.HTTP11)
                .baseUrl(url);
    }

    public <T> Flux<T> request(HttpMethod method, String uri, Multimap<String, String> parameters, TypeToken<T> typeToken, String contentType) {
        QueryStringEncoder queryStringEncoder = new QueryStringEncoder(uri);
        parameters.entries().forEach(e -> queryStringEncoder.addParam(e.getKey(), e.getValue()));
        return httpClient
                .headers(httpHeaders -> httpHeaders.set(HttpHeaderNames.ACCEPT, contentType))
                .request(method)
                .uri(queryStringEncoder.toString())
                .response((res, flux) -> {

                    int code = res.status().code();
                    if (code >= 400) {
                        return Mono.error(new RuntimeException(res.status().reasonPhrase()));
                    }

                    LOGGER.info("Get response {}", res.status());
                    return codecManager.decodeAs(res, flux, typeToken);
                });
    }
}
