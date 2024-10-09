package com.xama;

import com.google.common.io.ByteStreams;
import com.xama.client.Credentials;

import java.io.IOException;
import java.util.UUID;

public final class TestUtils {
    private TestUtils(){}

    public static Credentials CREDENTIALS = new Credentials(
            "<access-token>",
            UUID.fromString("e57140f7-cb25-479b-8cd1-83c078c39ddc"), // org id
            "xama-xero-client-test"
    );

    public static byte[] toByteArray(final String filePathInClassPath) throws IOException {
        return ByteStreams.toByteArray(FilesClientIT.class.getResourceAsStream(filePathInClassPath));
    }
}
