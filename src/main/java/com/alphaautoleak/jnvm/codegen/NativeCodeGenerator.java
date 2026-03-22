package com.alphaautoleak.jnvm.codegen;

import com.alphaautoleak.jnvm.asm.MethodInfo;
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
    private final List<MethodInfo> protectedMethods;
    private final byte[] stringKey;
    private final String bridgeClass;
    private final int methodIdXorKey;
    private final boolean directNativeRewrite;
    private final OpcodeObfuscator opcodeObfuscator;

    public NativeCodeGenerator(ProtectConfig config,
                               List<EncryptedMethodData> methods,
                               List<MethodInfo> protectedMethods,
                               String bridgeClass,
                               int methodIdXorKey,
                               boolean directNativeRewrite,
                               OpcodeObfuscator opcodeObfuscator) {
        this.config = config;
        this.methods = methods;
        this.protectedMethods = protectedMethods;
        this.stringKey = StringEncryptor.generateStringKey();
        this.bridgeClass = bridgeClass;
        this.methodIdXorKey = methodIdXorKey;
        this.directNativeRewrite = directNativeRewrite;
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
        NativeGenerationPlan plan = NativeGenerationPlan.create(
                config,
                methods,
                protectedMethods,
                bridgeClass,
                methodIdXorKey,
                directNativeRewrite,
                encryptStrings,
                opcodeObfuscator,
                stringKey
        );

        for (NativeGenerationStep step : plan.getSteps()) {
            step.generate();
            System.out.println("  [+] " + step.getOutputLabel());
        }

        System.out.println("[CODEGEN] Generated " + plan.getGeneratedFileCount() + " files.");
    }

    /**
     * Gets string encryption key
     */
    public byte[] getStringKey() {
        return stringKey;
    }
}
