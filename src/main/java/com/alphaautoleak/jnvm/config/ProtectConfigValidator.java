package com.alphaautoleak.jnvm.config;

import java.io.File;
import java.io.IOException;

final class ProtectConfigValidator {

    private final ProtectConfigLoader loader = new ProtectConfigLoader();

    void validate(ProtectConfig config) throws IOException {
        loader.load(config);

        if (config.getInputJar() == null || !config.getInputJar().exists()) {
            throw new IllegalArgumentException("Input JAR not found: " + config.getInputJar());
        }

        if (config.getProtectRules().isEmpty()) {
            System.out.println("[WARN] No protect rules specified, protecting ALL methods.");
            config.getProtectRules().add("**");
        }

        ensureNativeDirExists(config.getNativeDir());
    }

    private void ensureNativeDirExists(File nativeDir) {
        if (nativeDir != null && !nativeDir.exists()) {
            nativeDir.mkdirs();
        }
    }
}
