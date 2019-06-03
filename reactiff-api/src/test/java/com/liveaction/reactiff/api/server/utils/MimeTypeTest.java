package com.liveaction.reactiff.api.server.utils;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
public final class MimeTypeTest {

    @Parameters(value = {
            "file.txt,text/plain",
            "file.html,text/html",
            "file.csv,text/csv",
            "file.css,text/css",
            "file.png,image/png",
            "file.jpg,image/jpeg",
            "file.jpeg,image/jpeg",
            "file.js,application/javascript",
            "file.json,application/json",
            "file.xml,application/xml",
            "file.yaml,application/x-yaml",
            "file,application/octet-stream",
            "file.idontknow,application/octet-stream"
    })
    @Test
    public void shouldGetMimeType(String fileName, String mimeType) {
        assertThat(new MimeType(fileName).get()).isEqualTo(mimeType);
    }
}