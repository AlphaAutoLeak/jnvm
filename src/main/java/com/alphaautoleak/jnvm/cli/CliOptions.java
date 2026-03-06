package com.alphaautoleak.jnvm.cli;

import org.apache.commons.cli.*;

/**
 * 命令行选项定义
 */
public class CliOptions {

    private static final String VERSION = "1.0.0";
    private static final String BANNER =
            "╔══════════════════════════════════════╗\n" +
            "║   JNVM - Java Native VM Protector   ║\n" +
            "║   v" + VERSION + "                            ║\n" +
            "╚══════════════════════════════════════╝";

    public static String getBanner() {
        return BANNER;
    }

    public static Options build() {
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
                .longOpt("debug")
                .desc("Enable debug logging in native VM")
                .hasArg().argName("BOOL")
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
                .longOpt("encrypt-strings")
                .desc("Enable ChaCha20 string encryption (default: true)")
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

    public static void printHelp(Options options) {
        new HelpFormatter().printHelp("jnvm", options, true);
    }
}
