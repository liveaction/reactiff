package com.liveaction.reactiff.server.param.converters;

import java.time.Instant;

public final class InstantParamConverter implements ParamConverter<Instant> {

    /**
     * The converter.
     */
    public static final InstantParamConverter INSTANCE = new InstantParamConverter();

    private InstantParamConverter() {
        // No direct instantiation
    }

    @Override
    public boolean canConvertType(Class<?> clazz) {
        return clazz == Instant.class;
    }

    @Override
    public Instant fromString(String input) throws IllegalArgumentException {
        if (input == null) {
            return null;
        }
        try {
            long ts = Long.parseLong(input);
            return Instant.ofEpochSecond(ts);
        } catch (NumberFormatException nfe) {
            return Instant.parse(input);
        }
    }
}
