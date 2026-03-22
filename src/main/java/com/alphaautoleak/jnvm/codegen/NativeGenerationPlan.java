package com.alphaautoleak.jnvm.codegen;

import com.alphaautoleak.jnvm.asm.MethodInfo;
import com.alphaautoleak.jnvm.codegen.emitter.ChaCha20Generator;
import com.alphaautoleak.jnvm.codegen.emitter.VmBridgeGenerator;
import com.alphaautoleak.jnvm.codegen.emitter.VmDataGenerator;
import com.alphaautoleak.jnvm.codegen.emitter.VmInterpreterGenerator;
import com.alphaautoleak.jnvm.codegen.emitter.VmTypesGenerator;
import com.alphaautoleak.jnvm.config.ProtectConfig;
import com.alphaautoleak.jnvm.crypto.EncryptedMethodData;
import com.alphaautoleak.jnvm.crypto.OpcodeObfuscator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class NativeGenerationPlan {

    private final List<NativeGenerationStep> steps;

    private NativeGenerationPlan(List<NativeGenerationStep> steps) {
        this.steps = steps;
    }

    static NativeGenerationPlan create(ProtectConfig config,
                                       List<EncryptedMethodData> methods,
                                       List<MethodInfo> protectedMethods,
                                       String bridgeClass,
                                       int methodIdXorKey,
                                       boolean directNativeRewrite,
                                       boolean encryptStrings,
                                       OpcodeObfuscator opcodeObfuscator,
                                       byte[] stringKey) {
        File dir = config.getNativeDir();
        List<NativeGenerationStep> steps = new ArrayList<>();
        steps.add(step("vm_types.h", () -> new VmTypesGenerator(dir, encryptStrings, opcodeObfuscator).generate()));
        steps.add(step("chacha20.h / chacha20.c", () -> new ChaCha20Generator(dir).generate()));
        steps.add(step(
                "vm_data.h / vm_data.c",
                () -> new VmDataGenerator(dir, methods, stringKey, encryptStrings).generate()));
        steps.add(step(
                "vm_interpreter.h / vm_interpreter.c",
                () -> new VmInterpreterGenerator(dir, config.isDebug(), encryptStrings, methodIdXorKey, opcodeObfuscator)
                        .generate()));
        steps.add(step(
                "vm_bridge.c",
                () -> new VmBridgeGenerator(
                        dir,
                        bridgeClass,
                        encryptStrings,
                        protectedMethods,
                        methodIdXorKey,
                        directNativeRewrite
                ).generate()));
        return new NativeGenerationPlan(steps);
    }

    List<NativeGenerationStep> getSteps() {
        return steps;
    }

    int getGeneratedFileCount() {
        return 7;
    }

    private static NativeGenerationStep step(String label, IOAction action) {
        return new NativeGenerationStep() {
            @Override
            public String getOutputLabel() {
                return label;
            }

            @Override
            public void generate() throws java.io.IOException {
                action.run();
            }
        };
    }

    private interface IOAction {
        void run() throws java.io.IOException;
    }
}
