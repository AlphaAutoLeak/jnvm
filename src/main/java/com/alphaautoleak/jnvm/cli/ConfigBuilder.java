package com.alphaautoleak.jnvm.cli;

import com.alphaautoleak.jnvm.config.ProtectConfig;
import org.apache.commons.cli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 从命令行参数构建配置
 * 支持通过 --config 指定 YAML 配置文件，命令行参数优先级高于配置文件
 */
public class ConfigBuilder {

    public static ProtectConfig build(CommandLine cmd) {
        ProtectConfig config = new ProtectConfig();

        // 先设置配置文件路径（用于后续 validate 中加载）
        setConfigFile(config, cmd);

        // 设置命令行参数（这些会覆盖配置文件中的值）
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
        // 如果没有指定，会从 YAML 配置文件加载
    }

    private static void setOutputJar(ProtectConfig config, CommandLine cmd) {
        if (cmd.hasOption("out")) {
            config.setOutputJar(new File(cmd.getOptionValue("out")));
        }
        // 如果没有指定，会在 validate 中根据 inputJar 自动生成
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
        // YAML 中的规则会在 validate 中合并进来
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