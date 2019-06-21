package com.liveaction.reactiff.codec;

import com.google.common.base.Throwables;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.Codec;
import com.liveaction.reactiff.api.server.Result;
import com.liveaction.reactiff.api.server.utils.MimeType;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

public final class RawFileCodec implements Codec {

    private static final TypeToken<File> FILE = TypeToken.of(File.class);
    private static final TypeToken<Path> PATH = TypeToken.of(Path.class);

    @Override
    public int rank() {
        return -5;
    }

    @Override
    public boolean supports(String contentType, TypeToken<?> typeToken) {
        return isFileOrPath(typeToken);
    }

    private boolean isFileOrPath(TypeToken<?> typeToken) {
        return typeToken != null && (FILE.isAssignableFrom(typeToken) || PATH.isAssignableFrom(typeToken));
    }

    @Override
    public <T> Mono<T> decodeMono(String contentType, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        return Mono.from(decode(byteBufFlux, typeToken));
    }

    @Override
    public <T> Flux<T> decodeFlux(String contentType, Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        return Flux.error(new IllegalArgumentException("Cannot get a flux from file"));
    }

    @SuppressWarnings("unchecked")
    private <T> Publisher<T> decode(Publisher<ByteBuf> byteBufFlux, TypeToken<T> typeToken) {
        if (isFileOrPath(typeToken)) {
            try {
                File file = File.createTempFile("reactiff-upload", ".raw");
                FileOutputStream out = new FileOutputStream(file);
                Mono<Void> writeFlux = Flux.from(byteBufFlux)
                        .concatMap(bytes -> {
                            try {
                                bytes.readBytes(out, bytes.readableBytes());
                            } catch (IOException e) {
                                return Mono.error(e);
                            }
                            return Mono.empty();
                        })
                        .then()
                        .doFinally(signalType -> {
                            try {
                                out.close();
                            } catch (IOException e) {
                                Throwables.propagate(e);
                            }
                        });
                if (FILE.isAssignableFrom(typeToken)) {
                    return writeFlux.thenReturn((T) file);
                } else {
                    return writeFlux.thenReturn((T) file.toPath());
                }
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        } else {
            throw new IllegalArgumentException("Unable to encode to type '" + typeToken + "'");
        }
    }

    @Override
    public <T> Publisher<ByteBuf> encode(String contentType, Publisher<T> data, TypeToken<T> typeToken) {
        Mono<Path> path;
        if (FILE.isAssignableFrom(typeToken)) {
            path = Mono.from(data)
                    .cast(File.class)
                    .map(File::toPath);
        } else if(PATH.isAssignableFrom(typeToken)) {
            path = Mono.from(data)
                    .cast(Path.class);
        } else {
            throw new IllegalArgumentException("Unable to encode type '" + typeToken + "'");
        }
        return path.flatMapMany(ByteBufFlux::fromPath);
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
                            .header(HttpHeaderNames.CONTENT_TYPE, new MimeType(fileName).toString(), false)
                            .build();
                });

    }
}
