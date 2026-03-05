package com.alphaautoleak.jnvm;

import com.alphaautoleak.jnvm.cli.CliOptions;
import com.alphaautoleak.jnvm.cli.ConfigBuilder;
import com.alphaautoleak.jnvm.config.ProtectConfig;
import com.alphaautoleak.jnvm.converter.Converter;
import org.apache.commons.cli.*;

public class Main {

    public static void main(String[] args) {
        System.out.println(CliOptions.getBanner());
        System.out.println();

        Options options = CliOptions.build();
        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                CliOptions.printHelp(options);
                return;
            }

            if (!cmd.hasOption("jar")) {
                System.err.println("[ERROR] --jar is required");
                CliOptions.printHelp(options);
                System.exit(1);
            }

            ProtectConfig config = ConfigBuilder.build(cmd);
            config.validate();

            printConfig(config);

            Converter converter = new Converter(config);
            converter.run();

            System.out.println();
            System.out.println("[SUCCESS] Protection complete.");

        } catch (ParseException e) {
            System.err.println("[ERROR] " + e.getMessage());
            CliOptions.printHelp(options);
            System.exit(1);
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
        System.out.println("  Global bridge:" + config.isGlobalBridge());
        System.out.println();
    }
}
