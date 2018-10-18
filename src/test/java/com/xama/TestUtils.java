package com.xama;

import com.google.common.io.ByteStreams;

import java.io.IOException;

public final class TestUtils {
    private TestUtils(){}

    public static byte[] toByteArray(final String filePathInClassPath) throws IOException {
        return ByteStreams.toByteArray(FilesClientIT.class.getResourceAsStream(filePathInClassPath));
    }

}
