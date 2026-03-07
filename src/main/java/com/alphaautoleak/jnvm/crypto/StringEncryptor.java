package com.alphaautoleak.jnvm.crypto;

/**
 * String encryption for constant pool.
 * Uses simple XOR + rotate scheme (lightweight, for C-side string obfuscation).
 *
 * Important strings (class/method/descriptor names) are not stored in plaintext in C source,
 * decrypted at runtime for JNI calls.
 */
public class StringEncryptor {

    /**
     * Encrypts string to byte array
     *
     * @param input original string
     * @param key   8-byte key
     * @return encrypted byte array
     */
    public static byte[] encrypt(String input, byte[] key) {
        byte[] data = input.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] result = new byte[data.length];

        for (int i = 0; i < data.length; i++) {
            byte k = key[i % key.length];
            // XOR + rotate
            result[i] = (byte) ((data[i] ^ k) + (i & 0xFF));
        }

        return result;
    }

    /**
     * Generates key for C-side decrypt function
     * Based on build-time random number
     */
    public static byte[] generateStringKey() {
        byte[] key = new byte[8];
        new java.security.SecureRandom().nextBytes(key);
        return key;
    }

    /**
     * Generates C-side decrypt function code
     */
    public static String generateDecryptFunction(byte[] key) {
        StringBuilder sb = new StringBuilder();
        sb.append("static void vm_decrypt_string(const unsigned char* enc, int len, char* out, const unsigned char* key) {\n");
        sb.append("    for (int i = 0; i < len; i++) {\n");
        sb.append("        out[i] = (char)((enc[i] - (i & 0xFF)) ^ key[i % 8]);\n");
        sb.append("    }\n");
        sb.append("    out[len] = '\\0';\n");
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Encrypts string and formats as C array literal
     */
    public static String toCEncryptedString(String input, byte[] key, String varName) {
        byte[] enc = encrypt(input, key);
        StringBuilder sb = new StringBuilder();
        sb.append("static const unsigned char ").append(varName).append("[] = ");
        sb.append(CryptoUtils.toCArrayLiteral(enc));
        sb.append(";\n");
        sb.append("static const int ").append(varName).append("_len = ").append(enc.length).append(";\n");
        return sb.toString();
    }
}