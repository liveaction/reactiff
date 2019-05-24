package com.liveaction.reactiff.server.internal.multipart;

import com.google.common.base.Throwables;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.liveaction.reactiff.api.server.multipart.FilePart;
import com.liveaction.reactiff.api.server.multipart.FormFieldPart;
import com.liveaction.reactiff.api.server.multipart.Part;
import org.synchronoss.cloud.nio.multipart.MultipartContext;
import org.synchronoss.cloud.nio.multipart.MultipartUtils;
import org.synchronoss.cloud.nio.multipart.NioMultipartParserListener;
import org.synchronoss.cloud.nio.stream.storage.StreamStorage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.netty.ByteBufFlux;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public final class FluxSinkMultipartListener implements NioMultipartParserListener {
    private static final OpenOption[] FILE_CHANNEL_OPTIONS =
            {StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE};

    private final FluxSink<Part> sink;

    private final MultipartContext context;

    private final AtomicInteger terminated = new AtomicInteger(0);

    public FluxSinkMultipartListener(FluxSink<Part> sink, MultipartContext context) {
        this.sink = sink;
        this.context = context;
    }

    @Override
    public void onPartFinished(StreamStorage storage, Map<String, List<String>> headers) {
        ListMultimap<String, String> httpHeaders = ArrayListMultimap.create();
        headers.forEach(httpHeaders::putAll);
        createPart(storage, Multimaps.asMap(httpHeaders)).ifPresent(this.sink::next);
    }

    private Optional<Part> createPart(StreamStorage storage, Map<String, List<String>> httpHeaders) {
        String filename = MultipartUtils.getFileName(httpHeaders);
        if (filename != null) {
            ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.builder();
            httpHeaders.forEach(builder::putAll);
            ImmutableListMultimap<String, String> headers = builder.build();
            return Optional.of(new FilePart() {
                @Override
                public String filename() {
                    return filename;
                }

                @Override
                public Mono<Void> transferTo(Path dest, Scheduler executor) {
                    return Mono.fromCallable(() -> {
                        ReadableByteChannel input = null;
                        FileChannel output = null;
                        try {
                            input = Channels.newChannel(storage.getInputStream());
                            output = FileChannel.open(dest, FILE_CHANNEL_OPTIONS);
                            long size = (input instanceof FileChannel ? ((FileChannel) input).size() : Long.MAX_VALUE);
                            long totalWritten = 0;
                            while (totalWritten < size) {
                                long written = output.transferFrom(input, totalWritten, size - totalWritten);
                                if (written <= 0) {
                                    break;
                                }
                                totalWritten += written;
                            }
                        }
                        catch (IOException ex) {
                            throw Throwables.propagate(ex);
                        }
                        finally {
                            if (input != null) {
                                try {
                                    input.close();
                                }
                                catch (IOException ignored) {
                                }
                            }
                            if (output != null) {
                                try {
                                    output.close();
                                }
                                catch (IOException ignored) {
                                }
                            }
                        }
                        return (Void) null;
                    }).subscribeOn(executor);
                }

                @Override
                public String name() {
                    return filename;
                }

                @Override
                public ListMultimap<String, String> headers() {
                    return headers;
                }

                @Override
                public ByteBufFlux content() {
                    ByteBuffer buffer = ByteBuffer.allocate(4096);
                    return ByteBufFlux.fromInbound(Flux.using(() -> Channels.newChannel(storage.getInputStream()),
                            fileChannel -> Flux.generate(sink -> {
                                try {
                                    int read = fileChannel.read(buffer);
                                    if (read > 0) {
                                        byte[] bytes = new byte[read];
                                        System.arraycopy(buffer.array(), buffer.arrayOffset(), bytes, 0, read);
                                        buffer.clear();
                                        sink.next(bytes);
                                    } else {
                                        sink.complete();
                                    }
                                } catch (IOException e) {
                                    sink.error(e);
                                }
                            })
                            , fileChannel -> {
                                try {
                                    fileChannel.close();
                                } catch (IOException e) {
                                }
                            }
                    ));
                }
            });
        }
        else if (MultipartUtils.isFormField(httpHeaders, this.context)) {
            String value = MultipartUtils.readFormParameterValue(storage, httpHeaders);
            String fieldName = MultipartUtils.getFieldName(httpHeaders);
            ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.builder();
            httpHeaders.forEach(builder::putAll);
            ImmutableListMultimap<String, String> headers = builder.build();
            return Optional.of(new FormFieldPart() {
                @Override
                public String value() {
                    return value;
                }

                @Override
                public String name() {
                    return fieldName;
                }

                @Override
                public ListMultimap<String, String> headers() {
                    return headers;
                }

                @Override
                public ByteBufFlux content() {
                    return ByteBufFlux.fromString(Mono.just(value));
                }
            });
        }
        else {
            return Optional.empty();
        }
    }

    @Override
    public void onError(String message, Throwable cause) {
        if (this.terminated.getAndIncrement() == 0) {
            this.sink.error(new RuntimeException(message, cause));
        }
    }

    @Override
    public void onAllPartsFinished() {
        if (this.terminated.getAndIncrement() == 0) {
            this.sink.complete();
        }
    }

    @Override
    public void onNestedPartStarted(Map<String, List<String>> headersFromParentPart) {
    }

    @Override
    public void onNestedPartFinished() {
    }
}
