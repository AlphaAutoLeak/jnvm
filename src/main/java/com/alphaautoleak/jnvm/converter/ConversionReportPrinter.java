package com.alphaautoleak.jnvm.converter;

import com.alphaautoleak.jnvm.compiler.ZigCompiler;
import com.alphaautoleak.jnvm.config.ProtectConfig;

final class ConversionReportPrinter {

    void printProtectionSummary(ProtectionSummary summary) {
        System.out.println();
        System.out.println("[INFO] Protection summary:");
        System.out.println("  Methods to protect: " + summary.getMethodCount());
        System.out.println("  Classes affected:   " + summary.getAffectedClassCount());
        System.out.println("  Total bytecode:     " + summary.getTotalBytecode() + " bytes");
        System.out.println("  Total metadata:     " + summary.getTotalMetadata() + " entries");
        System.out.println();
    }

    void printCompletion(ProtectConfig config,
                         ProtectionSummary summary,
                         ZigCompiler compiler,
                         long elapsedMillis) {
        System.out.println("[STEP 7/7] Done!");
        System.out.println();
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║         Protection Complete          ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.printf("║  Methods protected: %-15d ║%n", summary.getMethodCount());
        System.out.printf("║  Classes patched:   %-15d ║%n", summary.getAffectedClassCount());
        System.out.printf("║  Native libs:       %-15d ║%n", compiler.getOutputLibraries().size());
        System.out.printf("║  Time elapsed:      %-12s ms ║%n", elapsedMillis);
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  Output: " + padRight(config.getOutputJar().getName(), 27) + "║");
        System.out.println("╚══════════════════════════════════════╝");
    }

    private String padRight(String value, int width) {
        if (value.length() >= width) {
            return value.substring(0, width);
        }
        StringBuilder sb = new StringBuilder(value);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
