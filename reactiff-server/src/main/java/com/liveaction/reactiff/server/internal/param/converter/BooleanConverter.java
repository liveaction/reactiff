package com.liveaction.reactiff.server.internal.param.converter;

import com.liveaction.reactiff.server.param.converter.ParamTypeConverter;

public final class BooleanConverter implements ParamTypeConverter<Boolean> {

    public static final BooleanConverter INSTANCE = new BooleanConverter();

    private BooleanConverter() {
    }

    @Override
    public Boolean fromString(String s) {
        if (s == null) {
            return null;
        }
        return Boolean.valueOf(s);
    }

    @Override
    public boolean canConvertType(Class<?> clazz) {
        return clazz == Boolean.class;
    }
}
