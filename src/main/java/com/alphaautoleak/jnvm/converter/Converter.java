package com.alphaautoleak.jnvm.converter;

import com.alphaautoleak.jnvm.asm.JarScanner;
import com.alphaautoleak.jnvm.asm.MethodInfo;
import com.alphaautoleak.jnvm.cli.CliReporter;
import com.alphaautoleak.jnvm.codegen.NativeCodeGenerator;
import com.alphaautoleak.jnvm.compiler.ZigCompiler;
import com.alphaautoleak.jnvm.config.ProtectConfig;
import com.alphaautoleak.jnvm.crypto.BytecodeEncryptor;
import com.alphaautoleak.jnvm.crypto.EncryptedMethodData;
import com.alphaautoleak.jnvm.crypto.OpcodeObfuscator;
import com.alphaautoleak.jnvm.patcher.JarPatcher;
import com.alphaautoleak.jnvm.patcher.OutputPackager;

import java.util.List;

public class Converter {

    private final ProtectConfig config;
    private final ConversionReportPrinter reportPrinter = new ConversionReportPrinter();

    public Converter(ProtectConfig config) {
        this.config = config;
    }

    public void run() throws Exception {
        long startTime = System.currentTimeMillis();

        OpcodeObfuscator opcodeObfuscator = initializeOpcodeObfuscation();
        ConversionScanResult scanResult = scanInputJar(opcodeObfuscator);

        if (scanResult.isEmpty()) {
            reportPrinter.printNoMethodsMatched();
            return;
        }

        ProtectionSummary summary = ProtectionSummary.from(
                scanResult.getProtectedMethods(),
                scanResult.getAffectedClassCount()
        );
        reportPrinter.printProtectionSummary(summary);
        List<EncryptedMethodData> encryptedMethods = encryptBytecodeData(scanResult.getProtectedMethods());
        JarPatcher patcher = createJarPatcher(scanResult);
        ZigCompiler compiler = generateAndCompileNativeCode(
                scanResult,
                encryptedMethods,
                patcher,
                opcodeObfuscator
        );
        patchOutputJar(patcher);
        embedNativeLibraries(compiler);

        long elapsed = System.currentTimeMillis() - startTime;
        reportPrinter.printCompletion(config, summary, compiler, elapsed);
    }

    private OpcodeObfuscator initializeOpcodeObfuscation() {
        OpcodeObfuscator opcodeObfuscator = new OpcodeObfuscator();
        CliReporter.info("Opcode obfuscation enabled - each bytecode is mapped to random value");
        return opcodeObfuscator;
    }

    private ConversionScanResult scanInputJar(OpcodeObfuscator opcodeObfuscator) throws Exception {
        reportPrinter.printStep(ConversionStep.SCAN, String.valueOf(config.getInputJar()));
        JarScanner scanner = new JarScanner(config, opcodeObfuscator);
        List<MethodInfo> protectedMethods = scanner.scan(config.getInputJar());
        return ConversionScanResult.from(scanner, protectedMethods);
    }

    private List<EncryptedMethodData> encryptBytecodeData(List<MethodInfo> protectedMethods) {
        reportPrinter.printStep(ConversionStep.ENCRYPT);
        BytecodeEncryptor encryptor = new BytecodeEncryptor();
        List<EncryptedMethodData> encryptedMethods = encryptor.encryptAll(protectedMethods);
        reportPrinter.printSpacer();
        return encryptedMethods;
    }

    private JarPatcher createJarPatcher(ConversionScanResult scanResult) {
        return new JarPatcher(
                scanResult.getProtectedMethods(),
                scanResult.getAffectedClasses(),
                scanResult.getBootstrapMethodKeys(),
                config.isDirectNativeRewrite()
        );
    }

    private ZigCompiler generateAndCompileNativeCode(ConversionScanResult scanResult,
                                                     List<EncryptedMethodData> encryptedMethods,
                                                     JarPatcher patcher,
                                                     OpcodeObfuscator opcodeObfuscator) throws Exception {
        reportPrinter.printStep(ConversionStep.GENERATE);
        NativeCodeGenerator codegen = new NativeCodeGenerator(
                config,
                encryptedMethods,
                scanResult.getProtectedMethods(),
                patcher.getBridgeClass(),
                patcher.getMethodIdXorKey(),
                config.isDirectNativeRewrite(),
                opcodeObfuscator
        );
        codegen.generate();
        reportPrinter.printSpacer();

        reportPrinter.printStep(ConversionStep.COMPILE);
        ZigCompiler compiler = new ZigCompiler(config);
        compiler.compileAll();
        reportPrinter.printSpacer();
        return compiler;
    }

    private void patchOutputJar(JarPatcher patcher) throws Exception {
        reportPrinter.printStep(ConversionStep.PATCH);
        patcher.patch(config.getInputJar(), config.getOutputJar());
        reportPrinter.printSpacer();
    }

    private void embedNativeLibraries(ZigCompiler compiler) throws Exception {
        reportPrinter.printStep(ConversionStep.PACKAGE);
        OutputPackager packager = new OutputPackager();
        packager.embedNativeLibraries(config.getOutputJar(), compiler.getOutputLibraries());
        reportPrinter.printSpacer();
    }
}
