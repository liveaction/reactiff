package com.liveaction.reactiff.server.internal.param;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.server.internal.param.converter.*;
import com.liveaction.reactiff.server.param.converter.ParamTypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public final class ParamConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParamConverter.class);

    private final Map<Type, ParamTypeConverter<?>> converters = Maps.newConcurrentMap();

    private static final ImmutableList<ParamTypeConverter<?>> DEFAULT_CONVERTERS = ImmutableList.of(
            StringConverter.INSTANCE,
            BooleanConverter.INSTANCE,
            InstantParamConverter.INSTANCE,
            CharacterConverter.INSTANCE,
            PathConverter.INSTANCE
    );

    public ParamConverter(Collection<ParamTypeConverter<?>> converters) {
        DEFAULT_CONVERTERS.forEach(this::addConverter);
        converters.forEach(this::addConverter);
    }

    public <T> void addConverter(ParamTypeConverter<T> converter) {
        Type type = ((ParameterizedType) converter.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];
        LOGGER.debug("Adding a converter for {}", type.getTypeName());
        this.converters.put(type, converter);
    }

    public void removeConverter(ParamTypeConverter<?> converter) {
        Type type = ((ParameterizedType) converter.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];
        LOGGER.debug("Removing a converter for {}", type.getTypeName());
        this.converters.remove(type, converter);
    }

    @SuppressWarnings("unchecked")
    public <T> T convertValue(List<String> input, TypeToken<T> typeToken) throws IllegalArgumentException {
        Class<T> rawType = (Class<T>) typeToken.getRawType();
        if (rawType.isArray()) {
            return createArray(input, rawType.getComponentType());
        } else if (Collection.class.isAssignableFrom(rawType)) {
            return createCollection(input, typeToken);
        } else {
            return (input != null && input.size() != 0) ? convertSingleValue(input.get(0), rawType) : null;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T createCollection(Collection<String> input, TypeToken<T> typeToken) {
        // Get the generic type of the list
        TypeToken<T> itemType = (TypeToken<T>) typeToken.resolveType(Collection.class.getTypeParameters()[0]);
        ParamTypeConverter<T> converter = (ParamTypeConverter<T>) getConverter(itemType.getRawType());
        return createCollectionWithConverter(input, typeToken, converter);
    }

    @SuppressWarnings("unchecked")
    private <A, T> T createCollectionWithConverter(Collection<String> input, TypeToken<T> type, ParamTypeConverter<A> converter) {
        Collection<A> collection = (Collection<A>) initCollection(type.getRawType());
        if (input != null) {
            for (String v : input) {
                collection.add(converter.fromString(v));
            }
        }
        return (T) collection;
    }

    private <A> Collection<A> initCollection(Class<A> rawType) {
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
    private <T> T createArray(Collection<String> input, Class<?> componentType) {
        if (input == null) {
            return (T) Array.newInstance(componentType, 0);
        }

        Class<?> theType = componentType;
        if (componentType.isPrimitive()) {
            theType = Primitives.wrap(componentType);
        }

        ParamTypeConverter converter = getConverter(theType);

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

    private <T> T convertSingleValue(String input, Class<T> type) {
        if (type.isPrimitive()) {
            type = Primitives.wrap(type);
            if (input == null) {
                return null;
            }
        }

        ParamTypeConverter<T> converter = getConverter(type);
        return converter.fromString(input);
    }

    @SuppressWarnings("unchecked")
    private <T> ParamTypeConverter<T> getConverter(Class<T> type) {
        List<ParamTypeConverter<?>> paramConverters = Lists.newArrayList(converters.values());
        paramConverters.add(ConstructorBasedConverter.getFromType(type));
        paramConverters.add(MethodBasedConverter.getFromType(type));

        for (ParamTypeConverter<?> converter : paramConverters) {
            if (converter != null && converter.canConvertType(type)) {
                return (ParamTypeConverter<T>) converter;
            }
        }
        throw new NoSuchElementException(String.format("Cannot find a converter able to create instance of %s", type.getName()));
    }
}
