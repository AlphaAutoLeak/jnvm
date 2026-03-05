package com.alphaautoleak.jnvm.cli;

import com.alphaautoleak.jnvm.config.ProtectConfig;
import org.apache.commons.cli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 从命令行参数构建配置
 */
public class ConfigBuilder {

    public static ProtectConfig build(CommandLine cmd) {
        ProtectConfig config = new ProtectConfig();

        setInputJar(config, cmd);
        setOutputJar(config, cmd);
        setProtectRules(config, cmd);
        setTargets(config, cmd);
        setFlags(config, cmd);
        setPaths(config, cmd);

        return config;
    }

    private static void setInputJar(ProtectConfig config, CommandLine cmd) {
        config.setInputJar(new File(cmd.getOptionValue("jar")));
    }

    private static void setOutputJar(ProtectConfig config, CommandLine cmd) {
        if (cmd.hasOption("out")) {
            config.setOutputJar(new File(cmd.getOptionValue("out")));
        } else {
            String input = cmd.getOptionValue("jar");
            String out = input.replaceAll("\\.jar$", "-protected.jar");
            config.setOutputJar(new File(out));
        }
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
        config.setDebug(
                cmd.hasOption("debug") && Boolean.parseBoolean(cmd.getOptionValue("debug"))
        );

        config.setAntiDebug(
                !cmd.hasOption("anti-debug") || Boolean.parseBoolean(cmd.getOptionValue("anti-debug"))
        );

        config.setGlobalBridge(
                !cmd.hasOption("global-bridge") || Boolean.parseBoolean(cmd.getOptionValue("global-bridge"))
        );
    }

    private static void setPaths(ProtectConfig config, CommandLine cmd) {
        if (cmd.hasOption("config")) {
            config.setConfigFile(new File(cmd.getOptionValue("config")));
        }

        if (cmd.hasOption("native-dir")) {
            config.setNativeDir(new File(cmd.getOptionValue("native-dir")));
        } else {
            config.setNativeDir(new File("native"));
        }
    }
}