package com.alphaautoleak.jnvm.config;

import com.alphaautoleak.jnvm.cli.CliReporter;
import com.alphaautoleak.jnvm.cli.PlatformDetector;

import java.io.File;
import java.io.IOException;

/**
 * Loads config sources, applies defaults, validates inputs, and prepares runtime paths.
 */
public final class ProtectConfigBootstrap {

    private static final String DEFAULT_NATIVE_DIR = "native";
    private static final String DEFAULT_PROTECT_RULE = "**";

    private final ProtectConfigLoader loader = new ProtectConfigLoader();
    private final ProtectConfigValidator validator = new ProtectConfigValidator();

    public ProtectConfig load(String configPath) throws IOException {
        ProtectConfig config = new ProtectConfig();
        config.setConfigFile(new File(configPath));

        loader.load(config);
        applyDefaults(config);
        validator.validate(config);
        ensureNativeDirExists(config.getNativeDir());
        return config;
    }

    private void applyDefaults(ProtectConfig config) {
        if (config.getNativeDir() == null) {
            config.setNativeDir(resolveDefaultNativeDir(config));
        }

        if (config.getTargets().isEmpty()) {
            config.getTargets().add(PlatformDetector.detectCurrentTarget());
        }

        if (config.getProtectRules().isEmpty()) {
            CliReporter.warn("No protect rules specified, protecting ALL methods.");
            config.getProtectRules().add(DEFAULT_PROTECT_RULE);
        }

        if (config.getOutputJar() == null && config.getInputJar() != null) {
            config.setOutputJar(deriveOutputJar(config.getInputJar()));
        }
    }

    private File deriveOutputJar(File inputJar) {
        String fileName = inputJar.getName();
        String lowerFileName = fileName.toLowerCase();
        String outputName;

        if (lowerFileName.endsWith(".jar")) {
            outputName = fileName.substring(0, fileName.length() - 4) + "-obf.jar";
        } else {
            outputName = fileName + "-obf.jar";
        }

        File parentDir = inputJar.getAbsoluteFile().getParentFile();
        return parentDir == null ? new File(outputName) : new File(parentDir, outputName);
    }

    private File resolveDefaultNativeDir(ProtectConfig config) {
        File configFile = config.getConfigFile();
        if (configFile == null) {
            return new File(DEFAULT_NATIVE_DIR);
        }

        File configDir = configFile.getAbsoluteFile().getParentFile();
        if (configDir == null) {
            return new File(DEFAULT_NATIVE_DIR);
        }
        return new File(configDir, DEFAULT_NATIVE_DIR);
    }

    private void ensureNativeDirExists(File nativeDir) {
        if (nativeDir == null || nativeDir.exists()) {
            return;
        }
        if (!nativeDir.mkdirs() && !nativeDir.exists()) {
            throw new IllegalStateException("Failed to create native output directory: " + nativeDir);
        }
    }
}
