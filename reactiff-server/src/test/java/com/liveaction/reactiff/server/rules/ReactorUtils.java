package com.liveaction.reactiff.server.rules;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.reflect.TypeToken;
import com.liveaction.reactiff.api.codec.Body;
import com.liveaction.reactiff.server.internal.utils.ResultUtils;
import org.apache.commons.io.IOUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public final class ReactorUtils {

    private ReactorUtils() {
    }

    public static Mono<byte[]> asBinary(Flux<byte[]> data) {
        return ByteBufFlux.fromInbound(data).aggregate().asInputStream()
                .map(inputStream -> {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    try {
                        IOUtils.copy(inputStream, out);
                    } catch (IOException e) {
                        throw Throwables.propagate(e);
                    }
                    return out.toByteArray();
                });
    }

    public static Mono<String> asString(Flux<byte[]> data) {
        return asBinary(data).map(b -> new String(b, Charsets.UTF_8));
    }


    public static Body<byte[]> readFileAsFlux(String filename) {
        Flux<byte[]> flux = Flux.create(fluxSink -> {
            try {
                RandomAccessFile aFile = new RandomAccessFile(ResultUtils.class.getResource(filename).getFile(), "r");
                FileChannel inChannel = aFile.getChannel();
                ByteBuffer buffer = ByteBuffer.allocate(1024);
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
        return new Body<>(flux, TypeToken.of(byte[].class));
    }

    public static Body<File> readFile(String fileName) {
        return new Body<>(Mono.just(new File(ResultUtils.class.getResource(fileName).getFile())), TypeToken.of(File.class));
    }

}
