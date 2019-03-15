package com.liveaction.reactiff.server.internal.param.converter;

public interface ParamConverter<T> {

    T fromString(String s);

    boolean canConvertType(Class<?> clazz);

}
