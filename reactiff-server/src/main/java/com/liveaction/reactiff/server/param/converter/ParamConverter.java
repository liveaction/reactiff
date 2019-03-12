package com.liveaction.reactiff.server.param.converter;

public interface ParamConverter<T> {

    T fromString(String s);

    boolean canConvertType(Class<?> clazz);

}
