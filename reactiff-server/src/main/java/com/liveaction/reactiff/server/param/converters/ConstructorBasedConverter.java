package com.liveaction.reactiff.server.param.converters;

import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ConstructorBasedConverter<T> implements ParamConverter<T> {

    private final Constructor<T> constructor;
    private final Class<T> clazz;

    private ConstructorBasedConverter(Constructor<T> constructor, Class<T> clazz) {
        this.constructor = constructor;
        this.clazz = clazz;
    }
    @Override
    public T fromString(String input) {
        try {
            return constructor.newInstance(input);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            LoggerFactory.getLogger(this.getClass())
                    .error("Cannot create an instance of {} from \"{}\"",
                            constructor.getDeclaringClass().getName(),
                            input,
                            e);
            if (e.getCause() != null) {
                throw new IllegalArgumentException(e.getCause());
            } else {
                throw new IllegalArgumentException(e);
            }
        }
    }

    @Override
    public boolean canConvertType(Class<?> clazz) {
        return clazz.equals(this.clazz);
    }

    /**
     * Checks if the given class can be instanciated by a ConstructorBasedConverter (ie has a constructor taking a single String as argument)
     *
     * @param clazz the class
     * @return the MethodBasedConstructor if possible, null otherwise
     */
    public static <T> ConstructorBasedConverter<T> getFromType(Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getConstructor(String.class);
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            return new ConstructorBasedConverter<>(constructor, clazz);
        } catch (NoSuchMethodException e) {
            // The class does not have the right constructor, return null.
            return null;
        }
    }

}
