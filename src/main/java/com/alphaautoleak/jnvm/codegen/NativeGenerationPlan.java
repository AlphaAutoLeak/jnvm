package com.alphaautoleak.jnvm.codegen;

import com.alphaautoleak.jnvm.asm.MethodInfo;
import com.alphaautoleak.jnvm.codegen.emitter.ChaCha20Generator;
import com.alphaautoleak.jnvm.codegen.emitter.VmBridgeGenerator;
import com.alphaautoleak.jnvm.codegen.emitter.VmDataGenerator;
import com.alphaautoleak.jnvm.codegen.emitter.VmInterpreterGenerator;
import com.alphaautoleak.jnvm.codegen.emitter.VmTypesGenerator;
import com.alphaautoleak.jnvm.config.ProtectConfig;
import com.alphaautoleak.jnvm.converter.NativeBindingContext;
import com.alphaautoleak.jnvm.crypto.EncryptedMethodData;
import com.alphaautoleak.jnvm.crypto.OpcodeObfuscator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class NativeGenerationPlan {

    private final List<NativeGenerationStep> steps;

    private NativeGenerationPlan(List<NativeGenerationStep> steps) {
        this.steps = steps;
    }

    static NativeGenerationPlan create(ProtectConfig config,
                                       List<EncryptedMethodData> methods,
                                       List<MethodInfo> protectedMethods,
                                       NativeBindingContext bindingContext,
                                       boolean encryptStrings,
                                       OpcodeObfuscator opcodeObfuscator,
                                       byte[] stringKey) {
        File dir = config.getNativeDir();
        List<NativeGenerationStep> steps = new ArrayList<>();
        steps.add(step(Collections.singletonList("vm_types.h"),
                () -> new VmTypesGenerator(dir, encryptStrings, opcodeObfuscator).generate()));
        steps.add(step(Arrays.asList("chacha20.h", "chacha20.c"), () -> new ChaCha20Generator(dir).generate()));
        steps.add(step(
                Arrays.asList("vm_data.h", "vm_data.c"),
                () -> new VmDataGenerator(dir, methods, stringKey, encryptStrings).generate()));
        steps.add(step(
                Arrays.asList("vm_interpreter.h", "vm_interpreter.c"),
                () -> new VmInterpreterGenerator(
                        dir,
                        config.isDebug(),
                        encryptStrings,
                        bindingContext.getMethodIdXorKey(),
                        opcodeObfuscator)
                        .generate()));
        steps.add(step(
                Collections.singletonList("vm_bridge.c"),
                () -> new VmBridgeGenerator(
                        dir,
                        bindingContext.getBridgeClass(),
                        encryptStrings,
                        protectedMethods,
                        bindingContext.getMethodIdXorKey(),
                        bindingContext.isDirectNativeRewrite()
                ).generate()));
        return new NativeGenerationPlan(steps);
    }

    List<NativeGenerationStep> getSteps() {
        return steps;
    }

    int getGeneratedFileCount() {
        int count = 0;
        for (NativeGenerationStep step : steps) {
            count += step.getGeneratedFileCount();
        }
        return count;
    }

    private static NativeGenerationStep step(List<String> outputFiles, IOAction action) {
        final List<String> declaredOutputs = Collections.unmodifiableList(new ArrayList<>(outputFiles));
        return new NativeGenerationStep() {
            @Override
            public List<String> getOutputFiles() {
                return declaredOutputs;
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
