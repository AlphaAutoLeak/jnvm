package com.alphaautoleak.jnvm.converter;

import com.alphaautoleak.jnvm.asm.JarScanner;
import com.alphaautoleak.jnvm.asm.MethodInfo;
import com.alphaautoleak.jnvm.codegen.NativeCodeGenerator;
import com.alphaautoleak.jnvm.compiler.ZigCompiler;
import com.alphaautoleak.jnvm.config.ProtectConfig;
import com.alphaautoleak.jnvm.crypto.BytecodeEncryptor;
import com.alphaautoleak.jnvm.crypto.EncryptedMethodData;
import com.alphaautoleak.jnvm.crypto.OpcodeObfuscator;
import com.alphaautoleak.jnvm.patcher.JarPatcher;
import com.alphaautoleak.jnvm.patcher.OutputPackager;

import java.util.List;
import java.util.Set;

public class Converter {

    private final ProtectConfig config;
    private final ConversionReportPrinter reportPrinter = new ConversionReportPrinter();
    private OpcodeObfuscator opcodeObfuscator;
    private List<MethodInfo> protectedMethods;
    private Set<String> affectedClasses;
    private Set<String> bootstrapMethodKeys;
    private List<EncryptedMethodData> encryptedMethods;

    public Converter(ProtectConfig config) {
        this.config = config;
    }

    public void run() throws Exception {
        long startTime = System.currentTimeMillis();

        initializeOpcodeObfuscation();
        scanInputJar();

        if (protectedMethods.isEmpty()) {
            System.out.println("[WARN] No methods matched protection rules. Nothing to do.");
            return;
        }

        ProtectionSummary summary = ProtectionSummary.from(protectedMethods, affectedClasses.size());
        reportPrinter.printProtectionSummary(summary);
        encryptBytecodeData();
        JarPatcher patcher = createJarPatcher();
        ZigCompiler compiler = generateAndCompileNativeCode(patcher);
        patchOutputJar(patcher);
        embedNativeLibraries(compiler);

        long elapsed = System.currentTimeMillis() - startTime;
        reportPrinter.printCompletion(config, summary, compiler, elapsed);
    }

    private void initializeOpcodeObfuscation() {
        opcodeObfuscator = new OpcodeObfuscator();
        System.out.println("[INFO] Opcode obfuscation enabled - each bytecode is mapped to random value");
    }

    private void scanInputJar() throws Exception {
        System.out.println("[STEP 1/7] Scanning JAR: " + config.getInputJar());
        JarScanner scanner = new JarScanner(config, opcodeObfuscator);
        protectedMethods = scanner.scan(config.getInputJar());
        affectedClasses = scanner.getAffectedClasses();
        bootstrapMethodKeys = scanner.getBootstrapMethodKeys();
    }

    private void encryptBytecodeData() {
        System.out.println("[STEP 2/7] Encrypting bytecode...");
        BytecodeEncryptor encryptor = new BytecodeEncryptor();
        encryptedMethods = encryptor.encryptAll(protectedMethods);
        System.out.println();
    }

    private JarPatcher createJarPatcher() {
        return new JarPatcher(
                protectedMethods,
                affectedClasses,
                bootstrapMethodKeys,
                config.isDirectNativeRewrite()
        );
    }

    private ZigCompiler generateAndCompileNativeCode(JarPatcher patcher) throws Exception {
        System.out.println("[STEP 3/7] Generating native C sources...");
        NativeCodeGenerator codegen = new NativeCodeGenerator(
                config,
                encryptedMethods,
                protectedMethods,
                patcher.getBridgeClass(),
                patcher.getMethodIdXorKey(),
                config.isDirectNativeRewrite(),
                opcodeObfuscator
        );
        codegen.generate();
        System.out.println();

        System.out.println("[STEP 4/7] Compiling with Zig...");
        ZigCompiler compiler = new ZigCompiler(config);
        compiler.compileAll();
        System.out.println();
        return compiler;
    }

    private void patchOutputJar(JarPatcher patcher) throws Exception {
        System.out.println("[STEP 5/7] Patching JAR classes...");
        patcher.patch(config.getInputJar(), config.getOutputJar());
        System.out.println();
    }

    private void embedNativeLibraries(ZigCompiler compiler) throws Exception {
        System.out.println("[STEP 6/7] Embedding native libraries...");
        OutputPackager packager = new OutputPackager();
        packager.embedNativeLibraries(config.getOutputJar(), compiler.getOutputLibraries());
        System.out.println();
    }
}
