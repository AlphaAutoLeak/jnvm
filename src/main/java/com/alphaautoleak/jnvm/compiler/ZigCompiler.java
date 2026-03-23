package com.alphaautoleak.jnvm.compiler;

import com.alphaautoleak.jnvm.cli.CliReporter;
import com.alphaautoleak.jnvm.config.ProtectConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ZigCompiler {

    private final ProtectConfig config;
    private final List<File> outputLibraries = new ArrayList<>();
    private final ZigEnvironment environment = new ZigEnvironment();
    private final ZigCommandBuilder commandBuilder = new ZigCommandBuilder();
    private final ZigProcessRunner processRunner = new ZigProcessRunner();

    public ZigCompiler(ProtectConfig config) {
        this.config = config;
    }

    public void compileAll() throws Exception {
        environment.checkZigInstalled();

        String javaHome = environment.findJavaHome();
        CliReporter.taggedVerbose("ZIG", "JAVA_HOME = " + javaHome);

        for (String target : config.getTargets()) {
            CliReporter.tagged("ZIG", "Compiling for target: " + target);
            compileDirect(target, javaHome);
        }

        CliReporter.tagged("ZIG", "Compiled " + outputLibraries.size() + " libraries.");
    }

    /**
     * Compiles directly with zig cc - most reliable, no build.zig API dependency
     */
    private void compileDirect(String target, String javaHome) throws Exception {
        File nativeDir = config.getNativeDir();
        File outputFile = commandBuilder.buildOutputFile(target, nativeDir);
        List<String> cmd = commandBuilder.buildCompileCommand(target, javaHome, outputFile);
        String output = processRunner.runCompileProcess(target, nativeDir, cmd);
        verifyAndRecordOutput(target, outputFile, output);
    }

    private void verifyAndRecordOutput(String target, File outputFile, String output) {
        if (outputFile.exists()) {
            outputLibraries.add(outputFile);
            CliReporter.verbose("  [OK] " + outputFile.getAbsolutePath() +
                    " (" + (outputFile.length() / 1024) + " KB)");
            return;
        }
        throw new RuntimeException(
                "Output library not found: " + outputFile + "\nBuild output:\n" + output);
    }

    public List<File> getOutputLibraries() {
        return outputLibraries;
    }
}
