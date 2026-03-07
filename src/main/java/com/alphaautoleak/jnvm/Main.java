package com.alphaautoleak.jnvm;

import com.alphaautoleak.jnvm.cli.ConfigBuilder;
import com.alphaautoleak.jnvm.config.ProtectConfig;
import com.alphaautoleak.jnvm.converter.Converter;

public class Main {

    private static final String BANNER =
            "╔══════════════════════════════════════╗\n" +
            "║   JNVM - Java Native VM Protector   ║\n" +
            "║   v1.0.0                            ║\n" +
            "╚══════════════════════════════════════╝";

    public static void main(String[] args) {
        System.out.println(BANNER);
        System.out.println();

        if (args.length == 0) {
            System.err.println("[ERROR] Config file is required");
            System.out.println("Usage: jnvm <config.yml>");
            System.exit(1);
        }

        try {
            ProtectConfig config = ConfigBuilder.build(args[0]);
            config.validate();

            if (config.getOutputJar() == null && config.getInputJar() != null) {
                String input = config.getInputJar().getAbsolutePath();
                String out = input.replaceAll("\\.jar$", "-obf.jar");
                config.setOutputJar(new java.io.File(out));
            }

            printConfig(config);

            Converter converter = new Converter(config);
            converter.run();

            System.out.println();
            System.out.println("[SUCCESS] Protection complete.");

        } catch (Exception e) {
            System.err.println("[FATAL] " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }

    private static void printConfig(ProtectConfig config) {
        System.out.println("[INFO] Configuration:");
        System.out.println("  Input JAR:    " + config.getInputJar());
        System.out.println("  Output JAR:   " + config.getOutputJar());
        System.out.println("  Targets:      " + config.getTargets());
        System.out.println("  Protect rules:" + config.getProtectRules());
        System.out.println("  Anti-debug:   " + config.isAntiDebug());
        System.out.println("  Debug mode:   " + config.isDebug());
        System.out.println();
    }
}
