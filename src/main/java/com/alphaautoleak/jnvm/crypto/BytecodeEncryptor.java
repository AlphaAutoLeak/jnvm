package com.alphaautoleak.jnvm.crypto;

import com.alphaautoleak.jnvm.asm.MethodInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 封装方法字节码数据（不再加密字节码，ChaCha20 仅用于字符串加密）
 */
public class BytecodeEncryptor {

    public BytecodeEncryptor() {
        System.out.println("[CRYPTO] Bytecode encryption disabled (plaintext mode).");
    }

    /**
     * 处理所有方法
     *
     * @param methods 从 JarScanner 收集到的方法列表
     * @return 封装后的数据列表
     */
    public List<EncryptedMethodData> encryptAll(List<MethodInfo> methods) {
        List<EncryptedMethodData> result = new ArrayList<>();

        for (MethodInfo method : methods) {
            EncryptedMethodData data = new EncryptedMethodData(method);
            result.add(data);
            System.out.println("  [ENC] " + data);
        }

        System.out.println("[CRYPTO] Processed " + result.size() + " methods.");
        return result;
    }
}
