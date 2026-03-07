package com.alphaautoleak.jnvm.cli;

import com.alphaautoleak.jnvm.config.ProtectConfig;

import java.io.File;

/**
 * Builds config from YAML file
 */
public class ConfigBuilder {

    public static ProtectConfig build(String configPath) {
        ProtectConfig config = new ProtectConfig();
        
        // Set config file
        config.setConfigFile(new File(configPath));
        
        // Set default native dir if not specified in config
        if (config.getNativeDir() == null) {
            config.setNativeDir(new File("native"));
        }
        
        // Set default target if not specified in config
        if (config.getTargets().isEmpty()) {
            config.getTargets().add(PlatformDetector.detectCurrentTarget());
        }

        return config;
    }
}