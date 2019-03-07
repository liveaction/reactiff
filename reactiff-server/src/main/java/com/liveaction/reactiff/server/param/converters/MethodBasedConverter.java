package com.liveaction.reactiff.server.param.converters;

import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class MethodBasedConverter<T> implements ParamConverter<T> {
    public static final String FROM = "from";
    public static final String FROM_STRING = "fromString";
    public static final String VALUE_OF = "valueOf";
    private final Method method;
    private final Class<T> clazz;

    private MethodBasedConverter(Class<T> clazz, Method method) {
        this.clazz = clazz;
        this.method = method;
    }

    @Override
    public T fromString(String input) throws IllegalArgumentException {
        try {
            return clazz.cast(method.invoke(null, input)); // static method
        } catch (IllegalAccessException | InvocationTargetException e) {
            LoggerFactory.getLogger(this.getClass())
                    .error("Cannot create an instance of {} from \"{}\" using the '{}' method",
                            method.getDeclaringClass().getName(),
                            method.getName(),
                            input,
                            e);
            if (e.getCause() != null) {
                throw new IllegalArgumentException(e.getCause());
            } else {
                throw new IllegalArgumentException(e);
            }
        }
    }

    /**
     * Checks if the given class can be instanciated by a MethodBasedConverter (ie has a static 'valueOf', 'from' or 'fromString' method taking a single String as argument)
     *
     * @param clazz the class
     * @return the MethodBasedConstructor if possible, null otherwise
     */
    public static <T> MethodBasedConverter<T> getFromType(Class<T> clazz) {
        Method method;
        try {
            method = clazz.getMethod(VALUE_OF, String.class);
        } catch (NoSuchMethodException e) {
            try {
                method = clazz.getMethod(FROM, String.class);
            } catch (NoSuchMethodException e1) {
                try {
                    method = clazz.getMethod(FROM_STRING, String.class);
                } catch (NoSuchMethodException e2) {
                    // none of the 3 method maches, return null
                    return null;
                }
            }
        }

        if (Modifier.isStatic(method.getModifiers())) {
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            return new MethodBasedConverter<>(clazz, method);
        } else {
            // The from method is present but it must be static.
            return null;
        }
    }
}
