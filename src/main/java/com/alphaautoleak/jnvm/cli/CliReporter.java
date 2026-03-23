package com.alphaautoleak.jnvm.cli;

public final class CliReporter {

    private static volatile boolean verboseEnabled;

    private CliReporter() {
    }

    public static void configure(boolean verbose) {
        verboseEnabled = verbose;
    }

    public static boolean isVerbose() {
        return verboseEnabled;
    }

    public static void blank() {
        System.out.println();
    }

    public static void raw(String message) {
        System.out.println(message);
    }

    public static void rawf(String format, Object... args) {
        System.out.printf(format, args);
    }

    public static void verbose(String message) {
        if (verboseEnabled) {
            raw(message);
        }
    }

    public static void verbosef(String format, Object... args) {
        if (verboseEnabled) {
            rawf(format, args);
        }
    }

    public static void tagged(String tag, String message) {
        raw("[" + tag + "] " + message);
    }

    public static void taggedVerbose(String tag, String message) {
        if (verboseEnabled) {
            tagged(tag, message);
        }
    }

    public static void taggedError(String tag, String message) {
        System.err.println("[" + tag + "] " + message);
    }

    public static void info(String message) {
        tagged("INFO", message);
    }

    public static void warn(String message) {
        tagged("WARN", message);
    }

    public static void error(String message) {
        taggedError("ERROR", message);
    }

    public static void fatal(String message) {
        taggedError("FATAL", message);
    }

    public static void success(String message) {
        tagged("SUCCESS", message);
    }
}
