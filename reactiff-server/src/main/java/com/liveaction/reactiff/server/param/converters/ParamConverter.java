package com.liveaction.reactiff.server.param.converters;

public interface ParamConverter<T> {
    T fromString(String s);

    boolean canConvertType(Class<?> clazz);
}
