package com.liveaction.reactiff.api.server.utils;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

import javax.activation.MimetypesFileTypeMap;
import java.util.Objects;
import java.util.Optional;

public final class MimeType {

    private static ImmutableMap<String, String> MAIN_MAP = new ImmutableMap.Builder<String, String>()
            .put("txt", "text/plain")
            .put("html", "text/html")
            .put("csv", "text/csv")
            .put("css", "text/css")
            .put("png", "image/png")
            .put("jpg", "image/jpeg")
            .put("jpeg", "image/jpeg")
            .put("js", "application/javascript")
            .put("json", "application/json")
            .put("xml", "application/xml")
            .put("yaml", "application/x-yaml")
            .build();

    private static MimetypesFileTypeMap FALLBACK_MAP = new MimetypesFileTypeMap();

    private final String fileName;

    public MimeType(String fileName) {
        this.fileName = Objects.requireNonNull(fileName);
    }

    public String get() {
        int lastDotPosition = fileName.lastIndexOf(".");

        if (lastDotPosition > 0) {
            String fileExtension = fileName.substring(lastDotPosition + 1).toLowerCase();

            if (fileExtension.length() > 0) {
                return Optional.ofNullable(MAIN_MAP.get(fileExtension))
                        .orElse(FALLBACK_MAP.getContentType(fileName));
            }
        }

        return FALLBACK_MAP.getContentType(fileName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MimeType mimeType = (MimeType) o;
        return Objects.equals(fileName, mimeType.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("fileName", fileName)
                .toString();
    }
}
