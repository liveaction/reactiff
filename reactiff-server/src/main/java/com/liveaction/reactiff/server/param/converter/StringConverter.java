package com.liveaction.reactiff.server.param.converter;

public final class StringConverter implements ParamConverter<String> {

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
