package com.alphaautoleak.jnvm;

import com.alphaautoleak.jnvm.cli.ConfigBuilder;
import com.alphaautoleak.jnvm.cli.CliReporter;
import com.alphaautoleak.jnvm.config.ProtectConfig;
import com.alphaautoleak.jnvm.converter.Converter;

public class Main {

    private static final String BANNER =
            "╔══════════════════════════════════════╗\n" +
            "║   JNVM - Java Native VM Protector   ║\n" +
            "║   v1.0.0                            ║\n" +
            "╚══════════════════════════════════════╝";

    public static void main(String[] args) {
        CliReporter.configure(false);
        CliReporter.raw(BANNER);
        CliReporter.blank();

        if (args.length == 0) {
            CliReporter.error("Config file is required");
            CliReporter.raw("Usage: jnvm <config.yml>");
            System.exit(1);
        }

        try {
            ProtectConfig config = ConfigBuilder.build(args[0]);
            CliReporter.configure(config.isDebug());

            printConfig(config);

            Converter converter = new Converter(config);
            converter.run();

            CliReporter.blank();
            CliReporter.success("Protection complete.");

        } catch (Exception e) {
            CliReporter.fatal(e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }

    private static void printConfig(ProtectConfig config) {
        CliReporter.info("Configuration:");
        CliReporter.raw("  Input JAR:    " + config.getInputJar());
        CliReporter.raw("  Output JAR:   " + config.getOutputJar());
        CliReporter.raw("  Targets:      " + config.getTargets());
        CliReporter.raw("  Protect rules:" + config.getProtectRules());
        CliReporter.raw("  Debug mode:   " + config.isDebug());
        CliReporter.raw("  Direct native:" + config.isDirectNativeRewrite());
        CliReporter.blank();
    }
}
