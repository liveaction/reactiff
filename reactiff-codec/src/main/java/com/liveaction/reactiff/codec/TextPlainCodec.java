package com.liveaction.reactiff.codec;

import com.google.common.base.Charsets;
import com.google.common.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.netty.ByteBufFlux;

public final class TextPlainCodec implements Codec {

    public static final String TEXT_PLAIN = "text/plain";

    @Override
    public int rank() {
        return 0;
    }

    @Override
    public boolean supports(String contentType) {
        return TEXT_PLAIN.equals(contentType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Publisher<T> decode(String contentType, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        if (String.class.equals(typeToken.getRawType())) {
            return (Publisher<T>) ByteBufFlux.fromInbound(byteBufFlux).asString();
        } else {
            throw new IllegalArgumentException(TEXT_PLAIN + " deserialization is only compatible with Type String. Type " + typeToken + " is not supported");
        }
    }

    @Override
    public <T> Publisher<ByteBuf> encode(String contentType, Publisher<T> data) {
        return ByteBufFlux.fromInbound(Flux.from(data).map(t -> t.toString().getBytes(Charsets.UTF_8)));
    }

}
