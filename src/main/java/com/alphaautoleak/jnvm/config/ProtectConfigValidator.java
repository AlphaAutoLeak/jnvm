package com.alphaautoleak.jnvm.config;

final class ProtectConfigValidator {

    void validate(ProtectConfig config) {
        if (config.getInputJar() == null || !config.getInputJar().exists()) {
            throw new IllegalArgumentException("Input JAR not found: " + config.getInputJar());
        }
    }
}
