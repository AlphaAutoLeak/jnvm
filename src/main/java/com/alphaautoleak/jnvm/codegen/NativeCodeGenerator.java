package com.alphaautoleak.jnvm.codegen;

import com.alphaautoleak.jnvm.codegen.emitter.*;
import com.alphaautoleak.jnvm.config.ProtectConfig;
import com.alphaautoleak.jnvm.crypto.EncryptedMethodData;
import com.alphaautoleak.jnvm.crypto.StringEncryptor;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 生成所有 C 源文件 + build.zig
 * 
 * 重构后的版本，将各模块拆分到独立的生成器类中
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
     * 生成所有文件
     */
    public void generate() throws IOException {
        File dir = config.getNativeDir();
        if (!dir.exists()) dir.mkdirs();

        System.out.println("[CODEGEN] Output directory: " + dir.getAbsolutePath());

        // 1. vm_types.h
        new VmTypesGenerator(dir).generate();
        System.out.println("  [+] vm_types.h");

        // 2. chacha20.h / chacha20.c
        new ChaCha20Generator(dir).generate();
        System.out.println("  [+] chacha20.h / chacha20.c");

        // 3. vm_data.h / vm_data.c
        new VmDataGenerator(dir, methods, stringKey).generate();
        System.out.println("  [+] vm_data.h / vm_data.c");

        // 4. vm_interpreter.h / vm_interpreter.c
        new VmInterpreterGenerator(dir, config.isDebug(), methodIdXorKey).generate();
        System.out.println("  [+] vm_interpreter.h / vm_interpreter.c");

        // 5. vm_bridge.c
        new VmBridgeGenerator(dir, methods.size(), bridgeClass).generate();
        System.out.println("  [+] vm_bridge.c");

        // 6. build.zig
        String javaHome = System.getenv("JAVA_HOME");
        new BuildZigGenerator(dir, config.getTargets(), javaHome).generate();
        System.out.println("  [+] build.zig");

        System.out.println("[CODEGEN] Generated " + 8 + " files.");
    }

    /**
     * 获取字符串加密密钥
     */
    public byte[] getStringKey() {
        return stringKey;
    }
}
