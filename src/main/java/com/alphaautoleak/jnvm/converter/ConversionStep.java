package com.alphaautoleak.jnvm.converter;

enum ConversionStep {

    SCAN(1, "Scanning JAR"),
    ENCRYPT(2, "Encrypting bytecode..."),
    GENERATE(3, "Generating native C sources..."),
    COMPILE(4, "Compiling with Zig..."),
    PATCH(5, "Patching JAR classes..."),
    PACKAGE(6, "Embedding native libraries..."),
    COMPLETE(7, "Done!");

    private static final int TOTAL_STEPS = 7;

    private final int number;
    private final String title;

    ConversionStep(int number, String title) {
        this.number = number;
        this.title = title;
    }

    String format(String detail) {
        if (detail == null || detail.isEmpty()) {
            return String.format("[STEP %d/%d] %s", number, TOTAL_STEPS, title);
        }
        return String.format("[STEP %d/%d] %s: %s", number, TOTAL_STEPS, title, detail);
    }
}
