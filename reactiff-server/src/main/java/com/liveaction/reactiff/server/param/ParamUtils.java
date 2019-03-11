package com.liveaction.reactiff.server.param;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.server.param.converters.BooleanConverter;
import com.liveaction.reactiff.server.param.converters.CharacterConverter;
import com.liveaction.reactiff.server.param.converters.ConstructorBasedConverter;
import com.liveaction.reactiff.server.param.converters.InstantParamConverter;
import com.liveaction.reactiff.server.param.converters.MethodBasedConverter;
import com.liveaction.reactiff.server.param.converters.ParamConverter;
import com.liveaction.reactiff.server.param.converters.StringConverter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

public final class ParamUtils {

    private static List<ParamConverter<?>> converters = Lists.newArrayList(
            StringConverter.INSTANCE,
            BooleanConverter.INSTANCE,
            InstantParamConverter.INSTANCE,
            CharacterConverter.INSTANCE
    );

    private ParamUtils() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T convertValue(String input, TypeToken<T> typeToken) throws IllegalArgumentException {
        Class<T> rawType = (Class<T>) typeToken.getRawType();
        if (rawType.isArray()) {
            List<String> args = getMultipleValues(input);
            return createArray(args, rawType.getComponentType());
        } else if (Collection.class.isAssignableFrom(rawType)) {
            List<String> args = getMultipleValues(input);
            return createCollection(args, typeToken);
        } else {
            return convertSingleValue(input, rawType);
        }
    }

    private static List<String> getMultipleValues(String input) {
        if (input == null) {
            return null;
        }
        String[] segments = input.split(",");
        List<String> values = new ArrayList<>();
        for (String s : segments) {
            String v = s.trim();
            if (!v.isEmpty()) {
                values.add(v);
            }
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    private static <T> T createCollection(Collection<String> input, TypeToken<T> typeToken) {
        // Get the generic type of the list
        TypeToken<T> itemType = (TypeToken<T>) typeToken.resolveType(Collection.class.getTypeParameters()[0]);
        ParamConverter<T> converter = (ParamConverter<T>) getConverter(itemType.getRawType());
        return createCollectionWithConverter(input, typeToken, converter);
    }

    @SuppressWarnings("unchecked")
    private static <A, T> T createCollectionWithConverter(Collection<String> input, TypeToken<T> type, ParamConverter<A> converter) {
        Collection<A> collection = (Collection<A>) initCollection(type.getRawType());
        if (input != null) {
            for (String v : input) {
                collection.add(converter.fromString(v));
            }
        }
        return (T) collection;
    }

    private static <A> Collection<A> initCollection(Class<A> rawType) {
        if (rawType.isAssignableFrom(List.class)) {
            return Lists.newArrayList();
        }
        if (rawType.isAssignableFrom(Set.class)) {
            return Sets.newLinkedHashSet();
        } else {
            throw new IllegalArgumentException(String.format("Not supported collection type %s", rawType));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T createArray(Collection<String> input, Class<?> componentType) {
        if (input == null) {
            return (T) Array.newInstance(componentType, 0);
        }

        Class<?> theType = componentType;
        if (componentType.isPrimitive()) {
            theType = Primitives.wrap(componentType);
        }

        ParamConverter converter = getConverter(theType);

        List<Object> list = new ArrayList<>();
        for (String v : input) {
            list.add(converter.fromString(v));
        }
        // We cannot use the toArray method as the the type does not match (toArray would produce an object[]).
        Object array = Array.newInstance(componentType, list.size());
        int i = 0;
        for (Object o : list) {
            Array.set(array, i, o);
            i++;
        }

        return (T) array;
    }

    private static <T> T convertSingleValue(String input, Class<T> type) {
        if (type.isPrimitive()) {
            type = Primitives.wrap(type);
            if (input == null) {
                return null;
            }
        }

        ParamConverter<T> converter = getConverter(type);
        return converter.fromString(input);
    }

    @SuppressWarnings("unchecked")
    private static <T> ParamConverter<T> getConverter(Class<T> type) {
        ArrayList<ParamConverter<?>> paramConverters = Lists.newArrayList(converters);
        paramConverters.add(ConstructorBasedConverter.getFromType(type));
        paramConverters.add(MethodBasedConverter.getFromType(type));


        for (ParamConverter<?> converter : paramConverters) {
            if (converter != null && converter.canConvertType(type)) {
                return (ParamConverter<T>) converter;
            }
        }
        throw new NoSuchElementException(String.format("Cannot find a converter able to create instance of %s", type.getName()));
    }
}
