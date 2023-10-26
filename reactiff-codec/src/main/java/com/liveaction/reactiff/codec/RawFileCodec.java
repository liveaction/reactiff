package com.liveaction.reactiff.codec;

import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.Codec;
import com.liveaction.reactiff.api.server.Result;
import com.liveaction.reactiff.api.server.utils.MimeType;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RawFileCodec implements Codec {

    private static final Logger LOGGER = LoggerFactory.getLogger(RawFileCodec.class);

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
        return typeToken != null && (FILE.isSupertypeOf(typeToken) || PATH.isSupertypeOf(typeToken));
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
                                throw new RuntimeException(e);
                            }
                        });
                if (FILE.isSupertypeOf(typeToken)) {
                    return writeFlux.thenReturn((T) file);
                } else {
                    return writeFlux.thenReturn((T) file.toPath());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("Unable to encode to type '" + typeToken + "'");
        }
    }

    @Override
    public <T> Publisher<ByteBuf> encode(String contentType, Publisher<T> data, TypeToken<T> typeToken) {
        Mono<Path> path;
        if (FILE.isSupertypeOf(typeToken)) {
            path = Mono.from(data)
                    .cast(File.class)
                    .map(File::toPath);
        } else if (PATH.isSupertypeOf(typeToken)) {
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
                    long size = -1;
                    Path path;
                    if (item instanceof File) {
                        path = ((File) item).toPath();
                    } else if (item instanceof Path) {
                        path = ((Path) item);
                    } else {
                        throw new IllegalArgumentException("Only Path and File is accepted");
                    }
                    String fileName = path.getFileName().toString();
                    try {
                        size = Files.size(path);
                    } catch (IOException e) {
                        LOGGER.debug("Cannot get size of {}", item);
                    }
                    Result.Builder<T> resultBuilder = result.copy()
                            .data((Publisher<T>) Mono.just(item), result.type())
                            .header(HttpHeaderNames.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                            .header(HttpHeaderNames.CONTENT_TYPE, new MimeType(fileName).toString(), false);
                    if (size != -1) {
                        resultBuilder
                                .header(HttpHeaderNames.CONTENT_LENGTH, Long.toString(size), false);
                    }
                    return resultBuilder.build();
                });

    }
}
