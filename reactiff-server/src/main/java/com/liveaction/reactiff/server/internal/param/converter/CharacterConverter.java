package com.liveaction.reactiff.server.internal.param.converter;

import com.liveaction.reactiff.server.param.converter.ParamTypeConverter;

public final class CharacterConverter implements ParamTypeConverter<Character> {

    public static final CharacterConverter INSTANCE = new CharacterConverter();

    private CharacterConverter() {
    }

    @Override
    public Character fromString(String input) {
        if (input == null) {
            throw new NullPointerException("input must not be null");
        }

        if (input.length() != 1) {
            throw new IllegalArgumentException(String.format("The input string \"%s\" is not a character. The length must be equal to", input));
        }
        return input.toCharArray()[0];
    }

    @Override
    public boolean canConvertType(Class<?> clazz) {
        return clazz == Character.class;
    }
}
