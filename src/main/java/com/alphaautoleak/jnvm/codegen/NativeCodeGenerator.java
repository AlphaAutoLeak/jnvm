package com.alphaautoleak.jnvm.codegen;

import com.alphaautoleak.jnvm.codegen.emitter.*;
import com.alphaautoleak.jnvm.config.ProtectConfig;
import com.alphaautoleak.jnvm.crypto.EncryptedMethodData;
import com.alphaautoleak.jnvm.crypto.StringEncryptor;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Generates native C code from Java bytecode.
 * Generates all C source files
 * 
 * Refactored version with modules split into independent generator classes
 */
public class NativeCodeGenerator {

    private final ProtectConfig config;
    private final List<EncryptedMethodData> methods;
    private final byte[] stringKey;
    private final String bridgeClass;
    private final int methodIdXorKey;

    public NativeCodeGenerator(ProtectConfig config, List<EncryptedMethodData> methods, String bridgeClass, int methodIdXorKey) {
        this.config = config;
        this.methods = methods;
        this.stringKey = StringEncryptor.generateStringKey();
        this.bridgeClass = bridgeClass;
        this.methodIdXorKey = methodIdXorKey;
    }

    /**
 * Generates native C code from Java bytecode.
     * Generates all files
     */
    public void generate() throws IOException {
        File dir = config.getNativeDir();
        if (!dir.exists()) dir.mkdirs();

        System.out.println("[CODEGEN] Output directory: " + dir.getAbsolutePath());

        boolean encryptStrings = config.isEncryptStrings();

        // 1. vm_types.h
        new VmTypesGenerator(dir, encryptStrings).generate();
        System.out.println("  [+] vm_types.h");

        // 2. chacha20.h / chacha20.c
        new ChaCha20Generator(dir).generate();
        System.out.println("  [+] chacha20.h / chacha20.c");

        // 3. vm_data.h / vm_data.c
        new VmDataGenerator(dir, methods, stringKey, encryptStrings).generate();
        System.out.println("  [+] vm_data.h / vm_data.c");

        // 4. vm_interpreter.h / vm_interpreter.c
        new VmInterpreterGenerator(dir, config.isDebug(), encryptStrings, methodIdXorKey).generate();
        System.out.println("  [+] vm_interpreter.h / vm_interpreter.c");

        // 5. vm_bridge.c
        new VmBridgeGenerator(dir, bridgeClass, encryptStrings).generate();
        System.out.println("  [+] vm_bridge.c");

        System.out.println("[CODEGEN] Generated " + 7 + " files.");
    }

    /**
 * Generates native C code from Java bytecode.
     * Gets string encryption key
     */
    public byte[] getStringKey() {
        return stringKey;
    }
}
