package com.liveaction.reactiff.server.internal.param.converter;

public final class BooleanConverter implements ParamConverter<Boolean> {

    public static final BooleanConverter INSTANCE = new BooleanConverter();

    private BooleanConverter() {
    }

    @Override
    public Boolean fromString(String s) {
        return Boolean.valueOf(s);
    }

    @Override
    public boolean canConvertType(Class<?> clazz) {
        return clazz == Boolean.class;
    }
}
