package com.alphaautoleak.jnvm.codegen;

import com.alphaautoleak.jnvm.asm.MethodInfo;
import com.alphaautoleak.jnvm.cli.CliReporter;
import com.alphaautoleak.jnvm.config.ProtectConfig;
import com.alphaautoleak.jnvm.converter.NativeBindingContext;
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
    private final NativeBindingContext bindingContext;
    private final OpcodeObfuscator opcodeObfuscator;

    public NativeCodeGenerator(ProtectConfig config,
                               List<EncryptedMethodData> methods,
                               List<MethodInfo> protectedMethods,
                               NativeBindingContext bindingContext,
                               OpcodeObfuscator opcodeObfuscator) {
        this.config = config;
        this.methods = methods;
        this.protectedMethods = protectedMethods;
        this.stringKey = StringEncryptor.generateStringKey();
        this.bindingContext = bindingContext;
        this.opcodeObfuscator = opcodeObfuscator;
    }

    /**
     * Generates native C code from Java bytecode.
     * Generates all files
     */
    public void generate() throws IOException {
        File dir = config.getNativeDir();
        if (!dir.exists()) dir.mkdirs();

        CliReporter.taggedVerbose("CODEGEN", "Output directory: " + dir.getAbsolutePath());

        boolean encryptStrings = config.isEncryptStrings();
        NativeGenerationPlan plan = NativeGenerationPlan.create(
                config,
                methods,
                protectedMethods,
                bindingContext,
                encryptStrings,
                opcodeObfuscator,
                stringKey
        );

        for (NativeGenerationStep step : plan.getSteps()) {
            step.generate();
            CliReporter.verbose("  [+] " + step.getOutputLabel());
        }

        CliReporter.tagged("CODEGEN", "Generated " + plan.getGeneratedFileCount() + " files.");
    }

    /**
     * Gets string encryption key
     */
    public byte[] getStringKey() {
        return stringKey;
    }
}
