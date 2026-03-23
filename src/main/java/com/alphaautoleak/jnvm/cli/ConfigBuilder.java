package com.alphaautoleak.jnvm.cli;

import com.alphaautoleak.jnvm.config.ProtectConfig;
import com.alphaautoleak.jnvm.config.ProtectConfigBootstrap;

import java.io.IOException;

/**
 * Builds a fully loaded and validated runtime config from a config path.
 */
public class ConfigBuilder {

    private static final ProtectConfigBootstrap BOOTSTRAP = new ProtectConfigBootstrap();

    private ConfigBuilder() {
    }

    public static ProtectConfig build(String configPath) throws IOException {
        return BOOTSTRAP.load(configPath);
    }
}
