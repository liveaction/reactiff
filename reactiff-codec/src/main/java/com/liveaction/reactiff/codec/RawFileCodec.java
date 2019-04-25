package com.liveaction.reactiff.codec;

import com.google.common.base.Throwables;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.Codec;
import com.liveaction.reactiff.api.server.Result;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

public final class RawFileCodec implements Codec {

    private static final TypeToken<File> FILE = TypeToken.of(File.class);
    private static final TypeToken<Path> PATH = TypeToken.of(Path.class);
    private static MimetypesFileTypeMap MIME_TYPE_MAP = new MimetypesFileTypeMap();

    @Override
    public int rank() {
        return -5;
    }

    @Override
    public boolean supports(String contentType, TypeToken<?> typeToken) {
        return typeToken != null && (FILE.isAssignableFrom(typeToken) || PATH.isAssignableFrom(typeToken));
    }

    @Override
    public <T> Mono<T> decodeMono(String contentType, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        return Mono.from(decode(byteBufFlux, typeToken));
    }

    @Override
    public <T> Flux<T> decodeFlux(String contentType, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        return Flux.error(new IllegalArgumentException("Cannot get a flux a file"));
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

    @Override
    public <T> Mono<Result<T>> enrich(Result<T> result) {
        return ((Mono<?>) result.data())
                .map(item -> {
                    String fileName;
                    if (item instanceof File) {
                        fileName = ((File) item).getName();
                    } else if (item instanceof Path) {
                        fileName = ((Path) item).getFileName().toString();
                    } else {
                        throw new IllegalArgumentException("Only Path and File is accepted");
                    }
                    return result.copy()
                            .header(HttpHeaderNames.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                            .header(HttpHeaderNames.CONTENT_TYPE, MIME_TYPE_MAP.getContentType(fileName), false)
                            .build();
                });

    }
}
