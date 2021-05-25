package com.liveaction.reactiff.server.example;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.liveaction.reactiff.api.server.HttpMethod;
import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.api.server.Request;
import com.liveaction.reactiff.api.server.Result;
import com.liveaction.reactiff.api.server.annotation.RequestBody;
import com.liveaction.reactiff.api.server.annotation.RequestMapping;
import com.liveaction.reactiff.server.mock.Pojo;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.util.CharsetUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import static com.liveaction.reactiff.api.server.HttpMethod.GET;

public final class PojoHandler implements ReactiveHandler {

    @RequestMapping(method = GET, path = "/pojo")
    public Flux<Pojo> list(Request request) {
        System.out.println(request.cookies());
        return Flux.range(0, 10)
                .delayElements(Duration.ofMillis(50))
                .map(index -> new Pojo(String.valueOf(index), "Hello you"));
    }

    @RequestMapping(method = GET, path = "/auth/saml/request")
    public Mono<Result> html() {
        // form to return
        StringBuilder formBuilder = new StringBuilder();
        formBuilder.append("<html>")
                .append("<body onload=\"document.forms[0].submit()\">")
                .append("<form method=\"post\" action=\"http://localhost:8080/simplesaml/saml2/idp/SSOService.php\">")
                .append("<input type=\"hidden\" name=\"SAMLRequest\" value=\"toto\" />")
                .append("</form>")
                .append("</body>")
                .append("</html>");
        final Result<String> res = Result.ok(Mono.just(formBuilder.toString()), String.class)
                .copy()
                .header(HttpHeaderNames.CONTENT_TYPE, "text/html")
                .build();
        return Mono.just(res);
    }

    @RequestMapping(method = HttpMethod.POST, path = "/auth/saml/login")
    public Mono<Result> login(Request request) {
        final String contentTypeHeader = request.header(HttpHeaderNames.CONTENT_TYPE);
        final Charset charset = HttpUtil.getCharset(contentTypeHeader, CharsetUtil.UTF_8);
        // get uri params, headers, cookies, ...
        return request.getFormData().map(o -> {
            getSAMLResponse(charset, o);
            // Use relaystate if coming from IdP initiated flow or get InReplyTo field when coming from SAMLRequest (SP initiated)
            final ImmutableList<String> relayStateList = o.get("RelayState");
            final String relayState = relayStateList.get(0);
            return Result.redirect(relayState)
                    .copy()
                    .cookie(PojoHandler.uiCookie("toto"))
                    .build();
        });
    }

    private void getSAMLResponse(Charset charset, ImmutableMap<String, ImmutableList<String>> o) {
        final ImmutableList<String> samlResponse = o.get("SAMLResponse");
        final String base64Saml = samlResponse.get(0);
        final byte[] decodedBytes = Base64.getDecoder().decode(base64Saml);
        final String samlString = new String(decodedBytes, charset);
        System.out.println(samlString);
    }

    public static Cookie uiCookie(String token) {
        DefaultCookie defaultCookie = new DefaultCookie("token", token);
        defaultCookie.setMaxAge(3600);
        defaultCookie.setSecure(false);
        defaultCookie.setHttpOnly(false);
        defaultCookie.setPath("/");
        return defaultCookie;
    }
}