package com.liveaction.reactiff.api.server.route;

import com.liveaction.reactiff.api.server.HttpMethod;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class RouteTest {

    @Test
    public void shouldTestUriParam() {
        HttpRoute route1 = new HttpRoute(0, HttpMethod.GET, "/test/nouriparam", null);
        HttpRoute route2 = new HttpRoute(0, HttpMethod.GET, "/test/{param}", null);
        Assertions.assertThat(route1.hasUriParam()).isFalse();
        Assertions.assertThat(route2.hasUriParam()).isTrue();
    }
}