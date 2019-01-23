package com.liveaction.reactiff.server.netty.annotation;

import com.liveaction.reactiff.server.netty.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {

    String path();

    HttpMethod[] method() default {
            HttpMethod.GET,
            HttpMethod.POST,
            HttpMethod.OPTIONS,
            HttpMethod.PUT,
            HttpMethod.HEAD,
            HttpMethod.DELETE
    };

    int rank() default 0;


}
