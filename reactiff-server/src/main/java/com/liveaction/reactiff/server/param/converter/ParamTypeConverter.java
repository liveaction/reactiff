package com.liveaction.reactiff.server.param.converter;

public interface ParamTypeConverter<T> {

    T fromString(String s);

    boolean canConvertType(Class<?> clazz);

}
