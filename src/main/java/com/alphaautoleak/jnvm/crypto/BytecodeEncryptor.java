package com.alphaautoleak.jnvm.crypto;

import com.alphaautoleak.jnvm.asm.MethodInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 对所有被保护方法的字节码进行加密。
 * 每个方法独立密钥 + nonce，互不影响。
 */
public class BytecodeEncryptor {

    /**
     * 加密所有方法
     *
     * @param methods 从 JarScanner 收集到的方法列表
     * @return 加密后的数据列表
     */
    public List<EncryptedMethodData> encryptAll(List<MethodInfo> methods) {
        // 先做自检
        if (!CryptoUtils.selfTest()) {
            throw new RuntimeException("ChaCha20 self-test FAILED! Aborting.");
        }
        System.out.println("[CRYPTO] ChaCha20 self-test passed.");

        List<EncryptedMethodData> result = new ArrayList<>();

        for (MethodInfo method : methods) {
            EncryptedMethodData encrypted = encryptMethod(method);
            result.add(encrypted);

            System.out.println("  [ENC] " + encrypted);
        }

        System.out.println("[CRYPTO] Encrypted " + result.size() + " methods.");
        return result;
    }

    /**
     * 加密单个方法
     */
    private EncryptedMethodData encryptMethod(MethodInfo method) {
        byte[] plaintext = method.getBytecode();

        // 生成独立的密钥和 nonce
        byte[] key = CryptoUtils.generateKey();
        byte[] nonce = CryptoUtils.generateNonce();

        // 加密
        byte[] ciphertext = CryptoUtils.chacha20(key, nonce, 0, plaintext);

        // 验证：解密回来确认正确
        byte[] verify = CryptoUtils.chacha20(key, nonce, 0, ciphertext);
        for (int i = 0; i < plaintext.length; i++) {
            if (plaintext[i] != verify[i]) {
                throw new RuntimeException(
                        "Encryption verification failed for method: "
                                + method.getOwner() + "." + method.getName()
                );
            }
        }

        return new EncryptedMethodData(method, ciphertext, key, nonce);
    }
}