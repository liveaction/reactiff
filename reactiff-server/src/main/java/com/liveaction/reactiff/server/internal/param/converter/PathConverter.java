package com.liveaction.reactiff.server.internal.param.converter;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class PathConverter implements ParamConverter<Path> {

    public static final PathConverter INSTANCE = new PathConverter();

    private PathConverter() {
    }

    @Override
    public Path fromString(String s) {
        return Paths.get(s);
    }

    @Override
    public boolean canConvertType(Class<?> clazz) {
        return clazz == Boolean.class;
    }

}
