package com.liveaction.reactiff.codec.jackson.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;

public final class PojoJacksonModule extends SimpleModule {
    public PojoJacksonModule() {
        addSerializer(ModuledPojo.class, new JsonSerializer<ModuledPojo>() {
            @Override
            public void serialize(ModuledPojo value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(value.type + "_" + value.value);
            }
        });
    }
}
