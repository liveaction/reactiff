package com.liveaction.reactiff.server.internal.param.converter;

public interface ParamTypeConverter<T> {

    T fromString(String s);

    boolean canConvertType(Class<?> clazz);

}
