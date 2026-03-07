package com.alphaautoleak.jnvm.cli;

import com.alphaautoleak.jnvm.config.ProtectConfig;
import org.apache.commons.cli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds config from command line arguments
 * Supports YAML config file via --config, CLI args take precedence over config file
 */
public class ConfigBuilder {

    public static ProtectConfig build(CommandLine cmd) {
        ProtectConfig config = new ProtectConfig();

        // Set config file path first (for loading in validate)
        setConfigFile(config, cmd);

        // Set CLI args (these override values from config file)
        setInputJar(config, cmd);
        setOutputJar(config, cmd);
        setProtectRules(config, cmd);
        setTargets(config, cmd);
        setFlags(config, cmd);
        setNativeDir(config, cmd);

        return config;
    }

    private static void setConfigFile(ProtectConfig config, CommandLine cmd) {
        if (cmd.hasOption("config")) {
            config.setConfigFile(new File(cmd.getOptionValue("config")));
        }
    }

    private static void setInputJar(ProtectConfig config, CommandLine cmd) {
        if (cmd.hasOption("jar")) {
            config.setInputJar(new File(cmd.getOptionValue("jar")));
        }
        // If not specified, will load from YAML config file
    }

    private static void setOutputJar(ProtectConfig config, CommandLine cmd) {
        if (cmd.hasOption("out")) {
            config.setOutputJar(new File(cmd.getOptionValue("out")));
        }
        // If not specified, will auto-generate in validate based on inputJar
    }

    private static void setProtectRules(ProtectConfig config, CommandLine cmd) {
        List<String> rules = new ArrayList<>();
        if (cmd.hasOption("protect")) {
            String[] values = cmd.getOptionValues("protect");
            if (values != null) {
                for (String v : values) {
                    rules.add(v.trim());
                }
            }
        }
        config.setProtectRules(rules);
        // Rules from YAML will be merged in validate
    }

    private static void setTargets(ProtectConfig config, CommandLine cmd) {
        List<String> targets = new ArrayList<>();
        if (cmd.hasOption("target")) {
            for (String t : cmd.getOptionValue("target").split(",")) {
                targets.add(t.trim());
            }
        }
        if (targets.isEmpty()) {
            targets.add(PlatformDetector.detectCurrentTarget());
        }
        config.setTargets(targets);
    }

    private static void setFlags(ProtectConfig config, CommandLine cmd) {
        if (cmd.hasOption("debug")) {
            config.setDebug(Boolean.parseBoolean(cmd.getOptionValue("debug")));
        }

        if (cmd.hasOption("anti-debug")) {
            config.setAntiDebug(Boolean.parseBoolean(cmd.getOptionValue("anti-debug")));
        }

        if (cmd.hasOption("encrypt-strings")) {
            config.setEncryptStrings(Boolean.parseBoolean(cmd.getOptionValue("encrypt-strings")));
        }
    }

    private static void setNativeDir(ProtectConfig config, CommandLine cmd) {
        if (cmd.hasOption("native-dir")) {
            config.setNativeDir(new File(cmd.getOptionValue("native-dir")));
        } else if (config.getNativeDir() == null) {
            config.setNativeDir(new File("native"));
        }
    }
}