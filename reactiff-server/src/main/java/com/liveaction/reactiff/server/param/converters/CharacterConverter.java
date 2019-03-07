package com.liveaction.reactiff.server.param.converters;

public class CharacterConverter implements ParamConverter<Character> {

    /**
     * The converter.
     */
    public static final CharacterConverter INSTANCE = new CharacterConverter();

    private CharacterConverter() {
        // No direct instantiation
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
