package com.alphaautoleak.jnvm.patcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

final class JarStreams {

    private JarStreams() {
    }

    static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        copy(input, baos);
        return baos.toByteArray();
    }

    static void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
    }
}
