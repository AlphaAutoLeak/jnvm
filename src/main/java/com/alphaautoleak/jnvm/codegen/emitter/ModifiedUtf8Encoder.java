package com.alphaautoleak.jnvm.codegen.emitter;

final class ModifiedUtf8Encoder {

    private ModifiedUtf8Encoder() {
    }

    static byte[] encode(String value) {
        if (value == null || value.isEmpty()) {
            return new byte[0];
        }

        int len = value.length();
        byte[] out = new byte[len * 3];
        int idx = 0;
        for (int i = 0; i < len; i++) {
            int c = value.charAt(i);
            if (c == 0x0000) {
                out[idx++] = (byte) 0xC0;
                out[idx++] = (byte) 0x80;
            } else if (c <= 0x007F) {
                out[idx++] = (byte) c;
            } else if (c <= 0x07FF) {
                out[idx++] = (byte) (0xC0 | (c >> 6));
                out[idx++] = (byte) (0x80 | (c & 0x3F));
            } else {
                out[idx++] = (byte) (0xE0 | (c >> 12));
                out[idx++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                out[idx++] = (byte) (0x80 | (c & 0x3F));
            }
        }

        if (idx == out.length) {
            return out;
        }
        byte[] trimmed = new byte[idx];
        System.arraycopy(out, 0, trimmed, 0, idx);
        return trimmed;
    }
}
