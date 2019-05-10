package com.liveaction.reactiff.server.internal.param.converter;

import com.liveaction.reactiff.server.param.converter.ParamTypeConverter;

import java.time.Instant;

public final class InstantParamConverter implements ParamTypeConverter<Instant> {

    public static final InstantParamConverter INSTANCE = new InstantParamConverter();

    private InstantParamConverter() {
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
