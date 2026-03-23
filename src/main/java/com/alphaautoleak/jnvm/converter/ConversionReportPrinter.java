package com.alphaautoleak.jnvm.converter;

import com.alphaautoleak.jnvm.cli.CliReporter;
import com.alphaautoleak.jnvm.compiler.ZigCompiler;
import com.alphaautoleak.jnvm.config.ProtectConfig;

final class ConversionReportPrinter {

    void printStep(ConversionStep step) {
        CliReporter.raw(step.format(null));
    }

    void printStep(ConversionStep step, String detail) {
        CliReporter.raw(step.format(detail));
    }

    void printNoMethodsMatched() {
        CliReporter.warn("No methods matched protection rules. Nothing to do.");
    }

    void printSpacer() {
        CliReporter.blank();
    }

    void printProtectionSummary(ProtectionSummary summary) {
        CliReporter.blank();
        CliReporter.info("Protection summary:");
        CliReporter.raw("  Methods to protect: " + summary.getMethodCount());
        CliReporter.raw("  Classes affected:   " + summary.getAffectedClassCount());
        CliReporter.raw("  Total bytecode:     " + summary.getTotalBytecode() + " bytes");
        CliReporter.raw("  Total metadata:     " + summary.getTotalMetadata() + " entries");
        CliReporter.blank();
    }

    void printCompletion(ProtectConfig config,
                         ProtectionSummary summary,
                         ZigCompiler compiler,
                         long elapsedMillis) {
        printStep(ConversionStep.COMPLETE);
        CliReporter.blank();
        CliReporter.raw("╔══════════════════════════════════════╗");
        CliReporter.raw("║         Protection Complete          ║");
        CliReporter.raw("╠══════════════════════════════════════╣");
        CliReporter.rawf("║  Methods protected: %-15d ║%n", summary.getMethodCount());
        CliReporter.rawf("║  Classes patched:   %-15d ║%n", summary.getAffectedClassCount());
        CliReporter.rawf("║  Native libs:       %-15d ║%n", compiler.getOutputLibraries().size());
        CliReporter.rawf("║  Time elapsed:      %-12s ms ║%n", elapsedMillis);
        CliReporter.raw("╠══════════════════════════════════════╣");
        CliReporter.raw("║  Output: " + padRight(config.getOutputJar().getName(), 27) + "║");
        CliReporter.raw("╚══════════════════════════════════════╝");
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
