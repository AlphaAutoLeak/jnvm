package com.alphaautoleak.jnvm;

import com.alphaautoleak.jnvm.config.ProtectConfig;
import com.alphaautoleak.jnvm.converter.Converter;
import org.apache.commons.cli.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final String VERSION = "1.0.0";
    private static final String BANNER =
            "╔══════════════════════════════════════╗\n" +
                    "║   JNVM - Java Native VM Protector   ║\n" +
                    "║   v" + VERSION + "                            ║\n" +
                    "╚══════════════════════════════════════╝";

    public static void main(String[] args) {
        System.out.println(BANNER);
        System.out.println();

        Options options = buildOptions();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter help = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                help.printHelp("jnvm", options, true);
                return;
            }

            // 必需参数校验
            if (!cmd.hasOption("jar")) {
                System.err.println("[ERROR] --jar is required");
                help.printHelp("jnvm", options, true);
                System.exit(1);
            }

            // 构建配置
            ProtectConfig config = buildConfig(cmd);
            config.validate();

            System.out.println("[INFO] Configuration:");
            System.out.println("  Input JAR:    " + config.getInputJar());
            System.out.println("  Output JAR:   " + config.getOutputJar());
            System.out.println("  Targets:      " + config.getTargets());
            System.out.println("  Protect rules:" + config.getProtectRules());
            System.out.println("  Anti-debug:   " + config.isAntiDebug());
            System.out.println("  Global bridge:" + config.isGlobalBridge());
            System.out.println();

            // 执行转换
            Converter converter = new Converter(config);
            converter.run();

            System.out.println();
            System.out.println("[SUCCESS] Protection complete.");

        } catch (ParseException e) {
            System.err.println("[ERROR] " + e.getMessage());
            help.printHelp("jnvm", options, true);
            System.exit(1);
        } catch (Exception e) {
            System.err.println("[FATAL] " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }

    private static Options buildOptions() {
        Options options = new Options();

        options.addOption(Option.builder()
                .longOpt("jar")
                .desc("Input JAR file path")
                .hasArg().argName("FILE")
                .build());

        options.addOption(Option.builder()
                .longOpt("out")
                .desc("Output JAR file path (default: <input>-protected.jar)")
                .hasArg().argName("FILE")
                .build());

        options.addOption(Option.builder()
                .longOpt("protect")
                .desc("Protection rule: package.**, Class#method, or @annotation. Repeatable.")
                .hasArg().argName("RULE")
                .build());

        options.addOption(Option.builder()
                .longOpt("config")
                .desc("Protection config file (protect.conf)")
                .hasArg().argName("FILE")
                .build());

        options.addOption(Option.builder()
                .longOpt("target")
                .desc("Comma-separated Zig targets (e.g. x86_64-linux-gnu,aarch64-linux-android29)")
                .hasArg().argName("TARGETS")
                .build());

        options.addOption(Option.builder()
                .longOpt("anti-debug")
                .desc("Enable anti-debug protections (default: true)")
                .hasArg().argName("BOOL")
                .build());

        options.addOption(Option.builder()
                .longOpt("global-bridge")
                .desc("Use single global native entry point (default: true)")
                .hasArg().argName("BOOL")
                .build());

        options.addOption(Option.builder()
                .longOpt("native-dir")
                .desc("Output directory for native sources (default: ./native)")
                .hasArg().argName("DIR")
                .build());

        options.addOption(Option.builder()
                .longOpt("help")
                .desc("Show this help")
                .build());

        return options;
    }

    private static ProtectConfig buildConfig(CommandLine cmd) {
        ProtectConfig config = new ProtectConfig();

        config.setInputJar(new File(cmd.getOptionValue("jar")));

        if (cmd.hasOption("out")) {
            config.setOutputJar(new File(cmd.getOptionValue("out")));
        } else {
            String input = cmd.getOptionValue("jar");
            String out = input.replaceAll("\\.jar$", "-protected.jar");
            config.setOutputJar(new File(out));
        }

        // 保护规则 - 可多次指定
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

        if (cmd.hasOption("config")) {
            config.setConfigFile(new File(cmd.getOptionValue("config")));
        }

        // 目标平台
        List<String> targets = new ArrayList<>();
        if (cmd.hasOption("target")) {
            for (String t : cmd.getOptionValue("target").split(",")) {
                targets.add(t.trim());
            }
        }
        if (targets.isEmpty()) {
            // 默认：当前平台
            targets.add(detectCurrentTarget());
        }
        config.setTargets(targets);

        config.setAntiDebug(
                !cmd.hasOption("anti-debug") || Boolean.parseBoolean(cmd.getOptionValue("anti-debug"))
        );

        config.setGlobalBridge(
                !cmd.hasOption("global-bridge") || Boolean.parseBoolean(cmd.getOptionValue("global-bridge"))
        );

        if (cmd.hasOption("native-dir")) {
            config.setNativeDir(new File(cmd.getOptionValue("native-dir")));
        } else {
            config.setNativeDir(new File("native"));
        }

        return config;
    }

    /**
     * 检测当前运行平台，映射为 Zig target triple
     */
    private static String detectCurrentTarget() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();

        String zigArch;
        if (arch.contains("amd64") || arch.contains("x86_64")) {
            zigArch = "x86_64";
        } else if (arch.contains("aarch64") || arch.contains("arm64")) {
            zigArch = "aarch64";
        } else if (arch.contains("x86") || arch.contains("i386") || arch.contains("i686")) {
            zigArch = "x86";
        } else {
            zigArch = "x86_64"; // fallback
        }

        String zigOs;
        if (os.contains("linux")) {
            zigOs = "linux-gnu";
        } else if (os.contains("windows")) {
            zigOs = "windows-gnu";
        } else if (os.contains("mac") || os.contains("darwin")) {
            zigOs = "macos";
        } else {
            zigOs = "linux-gnu"; // fallback
        }

        return zigArch + "-" + zigOs;
    }
}