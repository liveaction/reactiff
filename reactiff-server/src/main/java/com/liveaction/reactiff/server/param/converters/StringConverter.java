package com.liveaction.reactiff.server.param.converters;

public class StringConverter implements ParamConverter<String> {

    public static final StringConverter INSTANCE = new StringConverter();

    private StringConverter() {
    }

    @Override
    public String fromString(String s) {
        return s;
    }
}
