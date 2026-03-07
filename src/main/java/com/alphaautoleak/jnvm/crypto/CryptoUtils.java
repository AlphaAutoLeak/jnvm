package com.alphaautoleak.jnvm.crypto;

import java.security.SecureRandom;

/**
 * ChaCha20 encryption utility.
 *
 * Pure Java implementation of ChaCha20 stream cipher (RFC 7539).
 * No JCE Provider dependency, ensures all Java version compatibility.
 */
public class CryptoUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates random key
     * @return 32 bytes (256 bit)
     */
    public static byte[] generateKey() {
        byte[] key = new byte[32];
        SECURE_RANDOM.nextBytes(key);
        return key;
    }

    /**
     * Generates random nonce
     * @return 12 bytes (96 bit)
     */
    public static byte[] generateNonce() {
        byte[] nonce = new byte[12];
        SECURE_RANDOM.nextBytes(nonce);
        return nonce;
    }

    /**
     * ChaCha20 encryption (encryption and decryption are same operation)
     *
     * @param key       32 bytes
     * @param nonce     12 bytes
     * @param counter   initial counter (usually 0 or 1)
     * @param input     plaintext/ciphertext
     * @return          ciphertext/plaintext
     */
    public static byte[] chacha20(byte[] key, byte[] nonce, int counter, byte[] input) {
        if (key.length != 32) throw new IllegalArgumentException("Key must be 32 bytes");
        if (nonce.length != 12) throw new IllegalArgumentException("Nonce must be 12 bytes");

        byte[] output = new byte[input.length];
        int blocks = (input.length + 63) / 64;

        for (int i = 0; i < blocks; i++) {
            int[] state = initState(key, nonce, counter + i);
            int[] keyStream = chacha20Block(state);

            int offset = i * 64;
            int len = Math.min(64, input.length - offset);

            for (int j = 0; j < len; j++) {
                int wordIndex = j / 4;
                int byteIndex = j % 4;
                byte ksb = (byte) (keyStream[wordIndex] >>> (byteIndex * 8));
                output[offset + j] = (byte) (input[offset + j] ^ ksb);
            }
        }

        return output;
    }

    /**
     * Initializes ChaCha20 state matrix
     */
    private static int[] initState(byte[] key, byte[] nonce, int counter) {
        int[] state = new int[16];

        // Constant "expand 32-byte k"
        state[0] = 0x61707865;
        state[1] = 0x3320646e;
        state[2] = 0x79622d32;
        state[3] = 0x6b206574;

        // Key (8 32-bit words)
        for (int i = 0; i < 8; i++) {
            state[4 + i] = littleEndianToInt(key, i * 4);
        }

        // Counter
        state[12] = counter;

        // Nonce (3 32-bit words)
        for (int i = 0; i < 3; i++) {
            state[13 + i] = littleEndianToInt(nonce, i * 4);
        }

        return state;
    }

    /**
     * ChaCha20 block function: 20 rounds (10 double-rounds)
     */
    private static int[] chacha20Block(int[] input) {
        int[] state = input.clone();

        // 20 rounds = 10 double-rounds
        for (int i = 0; i < 10; i++) {
            // Column rounds
            quarterRound(state, 0, 4, 8, 12);
            quarterRound(state, 1, 5, 9, 13);
            quarterRound(state, 2, 6, 10, 14);
            quarterRound(state, 3, 7, 11, 15);
            // Diagonal rounds
            quarterRound(state, 0, 5, 10, 15);
            quarterRound(state, 1, 6, 11, 12);
            quarterRound(state, 2, 7, 8, 13);
            quarterRound(state, 3, 4, 9, 14);
        }

        // Add initial state
        for (int i = 0; i < 16; i++) {
            state[i] += input[i];
        }

        return state;
    }

    /**
     * Quarter round
     */
    private static void quarterRound(int[] s, int a, int b, int c, int d) {
        s[a] += s[b]; s[d] ^= s[a]; s[d] = Integer.rotateLeft(s[d], 16);
        s[c] += s[d]; s[b] ^= s[c]; s[b] = Integer.rotateLeft(s[b], 12);
        s[a] += s[b]; s[d] ^= s[a]; s[d] = Integer.rotateLeft(s[d], 8);
        s[c] += s[d]; s[b] ^= s[c]; s[b] = Integer.rotateLeft(s[b], 7);
    }

    /**
     * Little-endian bytes to int
     */
    private static int littleEndianToInt(byte[] buf, int offset) {
        return (buf[offset] & 0xFF)
                | ((buf[offset + 1] & 0xFF) << 8)
                | ((buf[offset + 2] & 0xFF) << 16)
                | ((buf[offset + 3] & 0xFF) << 24);
    }

    /**
     * Formats byte array as C array string
     * e.g. {0x1a, 0x2b, 0x3c, ...}
     */
    public static String toCArrayLiteral(byte[] data) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < data.length; i++) {
            if (i > 0) sb.append(",");
            if (i % 16 == 0) sb.append("\n    ");
            sb.append(String.format("0x%02x", data[i] & 0xFF));
        }
        sb.append("\n}");
        return sb.toString();
    }

    /**
     * Simple self-test
     */
    public static boolean selfTest() {
        byte[] key = new byte[32];
        byte[] nonce = new byte[12];
        byte[] plaintext = "Hello ChaCha20 self-test!".getBytes();

        byte[] encrypted = chacha20(key, nonce, 0, plaintext);
        byte[] decrypted = chacha20(key, nonce, 0, encrypted);

        if (plaintext.length != decrypted.length) return false;
        for (int i = 0; i < plaintext.length; i++) {
            if (plaintext[i] != decrypted[i]) return false;
        }
        return true;
    }
}