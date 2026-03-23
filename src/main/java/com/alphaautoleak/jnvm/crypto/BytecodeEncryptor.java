package com.alphaautoleak.jnvm.crypto;

import com.alphaautoleak.jnvm.asm.MethodInfo;
import com.alphaautoleak.jnvm.cli.CliReporter;

import java.util.ArrayList;
import java.util.List;

/**
 * Packages method bytecode data (no longer encrypts bytecode, ChaCha20 only for string encryption)
 */
public class BytecodeEncryptor {

    public BytecodeEncryptor() {
        CliReporter.tagged("CRYPTO", "Bytecode encryption disabled (plaintext mode).");
    }

    /**
     * Processes all methods
     *
     * @param methods method list collected from JarScanner
     * @return packaged data list
     */
    public List<EncryptedMethodData> encryptAll(List<MethodInfo> methods) {
        List<EncryptedMethodData> result = new ArrayList<>();

        for (MethodInfo method : methods) {
            EncryptedMethodData data = new EncryptedMethodData(method);
            result.add(data);
            CliReporter.verbose("  [ENC] " + data);
        }

        CliReporter.tagged("CRYPTO", "Processed " + result.size() + " methods.");
        return result;
    }
}
