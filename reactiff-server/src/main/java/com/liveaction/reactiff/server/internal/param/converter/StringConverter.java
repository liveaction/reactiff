package com.liveaction.reactiff.server.internal.param.converter;

import com.liveaction.reactiff.server.param.converter.ParamTypeConverter;

public final class StringConverter implements ParamTypeConverter<String> {

    public static final StringConverter INSTANCE = new StringConverter();

    private StringConverter() {
    }

    @Override
    public boolean canConvertType(Class<?> clazz) {
        return clazz == String.class;
    }

    @Override
    public String fromString(String s) {
        return s;
    }
}
