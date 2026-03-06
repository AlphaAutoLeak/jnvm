package com.alphaautoleak.jnvm.converter;

import com.alphaautoleak.jnvm.asm.JarScanner;
import com.alphaautoleak.jnvm.asm.MethodInfo;
import com.alphaautoleak.jnvm.codegen.NativeCodeGenerator;
import com.alphaautoleak.jnvm.compiler.ZigCompiler;
import com.alphaautoleak.jnvm.config.ProtectConfig;
import com.alphaautoleak.jnvm.crypto.BytecodeEncryptor;
import com.alphaautoleak.jnvm.crypto.EncryptedMethodData;
import com.alphaautoleak.jnvm.patcher.JarPatcher;
import com.alphaautoleak.jnvm.patcher.OutputPackager;

import java.io.File;
import java.util.List;
import java.util.Set;

public class Converter {

    private final ProtectConfig config;
    private List<MethodInfo> protectedMethods;
    private Set<String> affectedClasses;
    private List<EncryptedMethodData> encryptedMethods;

    public Converter(ProtectConfig config) {
        this.config = config;
    }

    public void run() throws Exception {
        long startTime = System.currentTimeMillis();

        // ===== STEP 1: 扫描 JAR =====
        System.out.println("[STEP 1/7] Scanning JAR: " + config.getInputJar());
        JarScanner scanner = new JarScanner(config);
        protectedMethods = scanner.scan(config.getInputJar());
        affectedClasses = scanner.getAffectedClasses();

        if (protectedMethods.isEmpty()) {
            System.out.println("[WARN] No methods matched protection rules. Nothing to do.");
            return;
        }

        System.out.println();
        System.out.println("[INFO] Protection summary:");
        System.out.println("  Methods to protect: " + protectedMethods.size());
        System.out.println("  Classes affected:   " + affectedClasses.size());
        int totalBytecode = 0;
        int totalMetadata = 0;
        for (MethodInfo m : protectedMethods) {
            totalBytecode += m.getBytecode().length;
            totalMetadata += m.getMetadata().size();
        }
        System.out.println("  Total bytecode:     " + totalBytecode + " bytes");
        System.out.println("  Total metadata:     " + totalMetadata + " entries");
        System.out.println();

        // ===== STEP 2: 加密字节码 =====
        System.out.println("[STEP 2/7] Encrypting bytecode...");
        BytecodeEncryptor encryptor = new BytecodeEncryptor(config.isEncryptBytecode());
        encryptedMethods = encryptor.encryptAll(protectedMethods);
        System.out.println();

        // ===== STEP 3: 创建 JarPatcher 获取随机 bridge 包名和 XOR key =====
        JarPatcher patcher = new JarPatcher(protectedMethods, affectedClasses);
        String bridgeClass = patcher.getBridgeClass();
        int methodIdXorKey = patcher.getMethodIdXorKey();

        // ===== STEP 4: 生成 C 源码 =====
        System.out.println("[STEP 3/7] Generating native C sources...");
        NativeCodeGenerator codegen = new NativeCodeGenerator(config, encryptedMethods, bridgeClass, methodIdXorKey);
        codegen.generate();
        System.out.println();

        // ===== STEP 5: Zig 编译 =====
        System.out.println("[STEP 4/7] Compiling with Zig...");
        ZigCompiler compiler = new ZigCompiler(config);
        compiler.compileAll();
        System.out.println();

        // ===== STEP 6: Patch JAR =====
        System.out.println("[STEP 5/7] Patching JAR classes...");
        patcher.patch(config.getInputJar(), config.getOutputJar());
        System.out.println();

        // ===== STEP 7: 嵌入 native 库 =====
        System.out.println("[STEP 6/7] Embedding native libraries...");
        OutputPackager packager = new OutputPackager();
        packager.embedNativeLibraries(config.getOutputJar(), compiler.getOutputLibraries());
        System.out.println();

        // ===== STEP 7: 完成 =====
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("[STEP 7/7] Done!");
        System.out.println();
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║         Protection Complete          ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.printf( "║  Methods protected: %-15d ║%n", protectedMethods.size());
        System.out.printf( "║  Classes patched:   %-15d ║%n", affectedClasses.size());
        System.out.printf( "║  Native libs:       %-15d ║%n", compiler.getOutputLibraries().size());
        System.out.printf( "║  Time elapsed:      %-12s ms ║%n", elapsed);
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  Output: " + padRight(config.getOutputJar().getName(), 27) + "║");
        System.out.println("╚══════════════════════════════════════╝");
    }

    private String padRight(String s, int width) {
        if (s.length() >= width) return s.substring(0, width);
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }
}