package com.liveaction.reactiff.server.general.example;

import com.google.common.base.Throwables;
import com.liveaction.reactiff.api.server.HttpMethod;
import com.liveaction.reactiff.api.server.ReactiveHandler;
import com.liveaction.reactiff.api.server.Result;
import com.liveaction.reactiff.api.server.annotation.RequestMapping;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FileTransferController implements ReactiveHandler {

    private final File tempFile;

    public FileTransferController(Path tmpFolder) {
        try {
            tempFile = Files.createFile(tmpFolder.resolve("table.csv")).toFile();
            Files.write(tempFile.toPath(), "test file".getBytes());
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @RequestMapping(method = HttpMethod.GET, path = "/download/file")
    public Result<File> downloadFile() {
        return Result.ok(Mono.just(tempFile), File.class);
    }

    @RequestMapping(method = HttpMethod.GET, path = "/download/path")
    public Result<Path> downloadPath() {
        return Result.ok(Mono.just(tempFile.toPath()), Path.class);
    }
}
