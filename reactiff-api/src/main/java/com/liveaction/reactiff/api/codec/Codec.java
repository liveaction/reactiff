package com.liveaction.reactiff.api.codec;

import com.google.common.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;

public interface Codec {

    int rank();

    boolean supports(String contentType);

    <T> Publisher<T> decode(String contentType, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken);

    <T> Publisher<ByteBuf> encode(String contentType, Publisher<T> data);

}
