package com.liveaction.reactiff.api.codec;

import com.google.common.reflect.TypeToken;
import org.reactivestreams.Publisher;

public final class Body<I> {

    public final Publisher<I> data;
    public final TypeToken<I> type;

    public Body(Publisher<I> data, TypeToken<I> type) {
        this.data = data;
        this.type = type;
    }

}
