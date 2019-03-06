package com.liveaction.reactiff.codec;

import com.google.common.base.Throwables;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.Codec;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public final class RawFileCodec implements Codec {

    private static final TypeToken<File> FILE = TypeToken.of(File.class);

    @Override
    public int rank() {
        return -5;
    }

    @Override
    public boolean supports(String contentType, TypeToken<?> typeToken) {
        return FILE.isAssignableFrom(typeToken);
    }

    @Override
    public <T> Mono<T> decodeMono(String contentType, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        return Mono.from(decode(byteBufFlux, typeToken));
    }

    @Override
    public <T> Flux<T> decodeFlux(String contentType, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        return Flux.from(decode(byteBufFlux, typeToken));
    }

    @Override
    public <T> T decodeEntity(String value, TypeToken<T> typeToken) {
        throw new UnsupportedOperationException("String value cannot be decoded by RawFileCodec");
    }

    @SuppressWarnings("unchecked")
    private <T> Publisher<T> decode(Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        if (FILE.isAssignableFrom(typeToken)) {
            try {
                File file = File.createTempFile("reactiff-upload", ".raw");
                FileOutputStream out = new FileOutputStream(file);
                return Flux.from(byteBufFlux)
                        .reduceWith(() -> out, (o, array) -> {
                            try {
                                array.readBytes(o, array.readableBytes());
                            } catch (IOException e) {
                                throw Throwables.propagate(e);
                            }
                            return o;
                        })
                        .map(o -> (T) file)
                        .doFinally(signalType -> {
                            try {
                                out.close();
                            } catch (IOException e) {
                                Throwables.propagate(e);
                            }
                        });
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        } else {
            throw new IllegalArgumentException("Unable to encode to type '" + typeToken + "'");
        }
    }

    @Override
    public <T> Publisher<ByteBuf> encode(String contentType, Publisher<T> data, TypeToken<T> typeToken) {
        if (FILE.isAssignableFrom(typeToken)) {
            return ByteBufFlux.fromInbound(Mono.from(data).flatMapMany(t -> readFileAsFlux((File) t)));
        } else {
            throw new IllegalArgumentException("Unable to encode type '" + typeToken + "'");
        }
    }

    private Flux<byte[]> readFileAsFlux(File file) {
        return Flux.create(fluxSink -> {
            try {
                RandomAccessFile aFile = new RandomAccessFile(file, "r");
                FileChannel inChannel = aFile.getChannel();
                ByteBuffer buffer = ByteBuffer.allocate(4096);
                while (inChannel.read(buffer) > 0) {
                    buffer.flip();
                    int len = buffer.limit();
                    byte[] bytes = new byte[len];
                    System.arraycopy(buffer.array(), buffer.arrayOffset(), bytes, 0, len);
                    buffer.clear(); // do something with the data and clear/compact it.
                    fluxSink.next(bytes);
                }
                inChannel.close();
                aFile.close();
                fluxSink.complete();
            } catch (IOException e) {
                fluxSink.error(e);
            }
        });
    }
}
