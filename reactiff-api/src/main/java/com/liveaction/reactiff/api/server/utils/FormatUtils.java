package com.liveaction.reactiff.api.server.utils;

import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.server.route.Route;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public final class FormatUtils {

    private FormatUtils() {
    }

    public static String formatRoutes(List<Route> routes) {
        int maxDescriptorLength = routes.stream().map(Route::descriptor).map(String::length).max(Comparator.naturalOrder()).orElse(0);
        int maxPathLength = routes.stream().map(Route::path).map(String::length).max(Comparator.naturalOrder()).orElse(0);
        int maxMethodName = routes.stream().map(r -> FormatUtils.formatMethodName(r.handlerMethod())).map(String::length).max(Comparator.naturalOrder()).orElse(0);
        return routes.stream()
                .map(r -> String.format("\t%-" + maxDescriptorLength + "s %-" + maxPathLength + "s => %-" + maxMethodName + "s : %s", r.descriptor(), r.path(), formatMethodName(r.handlerMethod()), formatReturnType(r.handlerMethod().getGenericReturnType())))
                .collect(Collectors.joining("\n"));
    }

    public static String formatReturnType(Type genericReturnType) {
        return formatClass(TypeToken.of(genericReturnType));
    }

    public static String formatMethodName(Method handlerMethod) {
        StringBuilder args = new StringBuilder();
        Parameter[] parameters = handlerMethod.getParameters();
        int parameterCount = handlerMethod.getParameterCount();
        for (int i = 0; i < parameterCount; i++) {
            Parameter parameter = parameters[i];
            args.append(parameter.getType().getSimpleName());
            args.append(" ");
            args.append(parameter.getName());
            if (i < (parameterCount - 1)) {
                args.append(", ");
            }
        }
        return handlerMethod.getDeclaringClass().getSimpleName() + "." + handlerMethod.getName() + "(" + args.toString() + ")";
    }

    private static String formatClass(TypeToken<?> typeToken) {
        Class<?> clazz = typeToken.getRawType();
        String types = Stream.of(clazz.getTypeParameters())
                .map(typeToken::resolveType)
                .map(FormatUtils::formatClass)
                .collect(joining(", "));
        if (types.length() > 0) {
            return clazz.getSimpleName() + '<' + types + '>';
        } else {
            return clazz.getSimpleName();
        }
    }

}
