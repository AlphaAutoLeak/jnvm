package com.alphaautoleak.jnvm.crypto;

import java.security.SecureRandom;

/**
 * String encryption key generator.
 * Used for method bytecode XOR obfuscation key.
 */
public class StringEncryptor {

    private StringEncryptor() {}

    /**
     * Generates random 8-byte key for XOR obfuscation
     */
    public static byte[] generateStringKey() {
        byte[] key = new byte[8];
        new SecureRandom().nextBytes(key);
        return key;
    }
}