package com.alphaautoleak.jnvm.codegen;

import com.alphaautoleak.jnvm.codegen.emitter.*;
import com.alphaautoleak.jnvm.config.ProtectConfig;
import com.alphaautoleak.jnvm.crypto.EncryptedMethodData;
import com.alphaautoleak.jnvm.crypto.OpcodeObfuscator;
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
    private final OpcodeObfuscator opcodeObfuscator;

    public NativeCodeGenerator(ProtectConfig config, List<EncryptedMethodData> methods, 
                               String bridgeClass, int methodIdXorKey, OpcodeObfuscator opcodeObfuscator) {
        this.config = config;
        this.methods = methods;
        this.stringKey = StringEncryptor.generateStringKey();
        this.bridgeClass = bridgeClass;
        this.methodIdXorKey = methodIdXorKey;
        this.opcodeObfuscator = opcodeObfuscator;
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

        // vm_types.h (includes opcode decode table)
        new VmTypesGenerator(dir, encryptStrings, opcodeObfuscator).generate();
        System.out.println("  [+] vm_types.h");

        // chacha20.h / chacha20.c
        new ChaCha20Generator(dir).generate();
        System.out.println("  [+] chacha20.h / chacha20.c");

        // vm_data.h / vm_data.c
        new VmDataGenerator(dir, methods, stringKey, encryptStrings).generate();
        System.out.println("  [+] vm_data.h / vm_data.c");

        // vm_interpreter.h / vm_interpreter.c
        new VmInterpreterGenerator(dir, config.isDebug(), encryptStrings, methodIdXorKey, opcodeObfuscator).generate();
        System.out.println("  [+] vm_interpreter.h / vm_interpreter.c");

        // vm_bridge.c
        new VmBridgeGenerator(dir, bridgeClass, encryptStrings).generate();
        System.out.println("  [+] vm_bridge.c");

        System.out.println("[CODEGEN] Generated " + 7 + " files.");
    }

    /**
     * Gets string encryption key
     */
    public byte[] getStringKey() {
        return stringKey;
    }
}
