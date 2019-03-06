package com.liveaction.reactiff.server.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liveaction.reactiff.api.codec.CodecManager;
import com.liveaction.reactiff.codec.CodecManagerImpl;
import com.liveaction.reactiff.codec.RawBinaryCodec;
import com.liveaction.reactiff.codec.RawFileCodec;
import com.liveaction.reactiff.codec.TextPlainCodec;
import com.liveaction.reactiff.codec.jackson.JsonCodec;
import com.liveaction.reactiff.codec.jackson.SmileBinaryCodec;
import org.junit.rules.ExternalResource;

public final class WithCodecManager extends ExternalResource {

    public CodecManager codecManager;

    private static ObjectMapper objectMapper = new ObjectMapper();

    public WithCodecManager() {
        codecManager = new CodecManagerImpl();
        codecManager.addCodec(new JsonCodec(objectMapper));
        codecManager.addCodec(new SmileBinaryCodec(objectMapper));
        codecManager.addCodec(new TextPlainCodec());
        codecManager.addCodec(new RawBinaryCodec());
        codecManager.addCodec(new RawFileCodec());
    }

}
