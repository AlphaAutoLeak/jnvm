package com.alphaautoleak.jnvm.compiler;

import com.alphaautoleak.jnvm.cli.CliReporter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

final class ZigEnvironment {

    private static final int ZIG_VERSION_TIMEOUT_SECONDS = 10;

    void checkZigInstalled() throws Exception {
        try {
            ProcessBuilder pb = new ProcessBuilder("zig", "version");
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            StringBuilder out = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line);
                }
            }

            proc.waitFor(ZIG_VERSION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            CliReporter.tagged("ZIG", "Zig version: " + out.toString().trim());
        } catch (IOException e) {
            throw new RuntimeException(
                    "Zig compiler not found. Please install Zig and add it to PATH.\n"
                            + "Download: https://ziglang.org/download/");
        }
    }

    String findJavaHome() {
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isEmpty() && new File(javaHome, "include").exists()) {
            return javaHome;
        }

        String javaHomeProp = System.getProperty("java.home");
        if (javaHomeProp != null) {
            File javaHomeDir = new File(javaHomeProp);
            if (new File(javaHomeDir, "include").exists()) {
                return javaHomeDir.getAbsolutePath();
            }
            File parent = javaHomeDir.getParentFile();
            if (parent != null && new File(parent, "include").exists()) {
                return parent.getAbsolutePath();
            }
        }

        throw new RuntimeException(
                "Cannot find JDK. Set JAVA_HOME environment variable to your JDK installation.");
    }
}
