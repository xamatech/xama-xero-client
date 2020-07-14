package com.xama;

import com.google.common.io.ByteStreams;
import com.xama.client.Credentials;

import java.io.IOException;
import java.util.UUID;

public final class TestUtils {
    private TestUtils(){}

    public static Credentials CREDENTIALS = new Credentials(
            "<access token>",
            UUID.fromString("1D91B189-C36F-4BB4-BD97-BB7E856254CB"), // org id
            "xama-xero-client-test"
    );

    public static byte[] toByteArray(final String filePathInClassPath) throws IOException {
        return ByteStreams.toByteArray(FilesClientIT.class.getResourceAsStream(filePathInClassPath));
    }

}
