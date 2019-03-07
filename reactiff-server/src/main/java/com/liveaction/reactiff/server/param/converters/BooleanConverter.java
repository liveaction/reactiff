package com.liveaction.reactiff.server.param.converters;

public class BooleanConverter implements ParamConverter<Boolean> {

    /**
     * The converter.
     */
    public static final BooleanConverter INSTANCE = new BooleanConverter();

    private BooleanConverter() {
        // No direct instantiation
    }

    @Override
    public Boolean fromString(String s) {
        return Boolean.valueOf(s);
    }
}
