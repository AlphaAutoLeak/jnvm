package com.alphaautoleak.jnvm.crypto;

/**
 * 常量池中字符串的加密。
 * 使用简单的 XOR + rotate 方案（轻量级，用于 C 端字符串混淆）。
 *
 * 重要字符串（类名/方法名/描述符）在 C 源码中不以明文存储，
 * 运行时解密后用于 JNI 调用。
 */
public class StringEncryptor {

    /**
     * 加密字符串为字节数组
     *
     * @param input 原始字符串
     * @param key   8 字节密钥
     * @return 加密后的字节数组
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
     * 生成 C 端解密函数使用的密钥
     * 基于 build-time 随机数
     */
    public static byte[] generateStringKey() {
        byte[] key = new byte[8];
        new java.security.SecureRandom().nextBytes(key);
        return key;
    }

    /**
     * 生成 C 端解密函数代码
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
     * 将字符串加密并格式化为 C 数组字面量
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