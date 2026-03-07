package com.alphaautoleak.jnvm.compiler;

import com.alphaautoleak.jnvm.config.ProtectConfig;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ZigCompiler {

    private final ProtectConfig config;
    private final List<File> outputLibraries = new ArrayList<>();

    public ZigCompiler(ProtectConfig config) {
        this.config = config;
    }

    public void compileAll() throws Exception {
        checkZigInstalled();

        String javaHome = findJavaHome();
        System.out.println("[ZIG] JAVA_HOME = " + javaHome);

        for (String target : config.getTargets()) {
            System.out.println("[ZIG] Compiling for target: " + target);
            compileDirect(target, javaHome);
        }

        System.out.println("[ZIG] Compiled " + outputLibraries.size() + " libraries.");
    }

    /**
     * Compiles directly with zig cc - most reliable, no build.zig API dependency
     */
    private void compileDirect(String target, String javaHome) throws Exception {
        File nativeDir = config.getNativeDir();

        // Output filename
        String libName;
        if (target.contains("windows")) {
            libName = "customvm.dll";
        } else if (target.contains("macos") || target.contains("darwin")) {
            libName = "libcustomvm.dylib";
        } else {
            libName = "libcustomvm.so";
        }

        File outputDir = new File(nativeDir, "out-" + target);
        outputDir.mkdirs();
        File outputFile = new File(outputDir, libName);

        List<String> cmd = new ArrayList<>();
        cmd.add("zig");
        cmd.add("cc");
        cmd.add("-target");
        cmd.add(target);

        cmd.add("-fno-sanitize=all");           // Disable all runtime sanitizers
        cmd.add("-fno-sanitize-trap=all");      // Prevent trap instructions for undefined behavior
        cmd.add("-O3");                         // Maximum speed optimization
        cmd.add("-ffast-math");                 // Aggressive float optimization
        cmd.add("-flto");                       // Link-time optimization
        cmd.add("-DNDEBUG");                    // Disable assertions

        cmd.add("-std=c11");
        cmd.add("-fPIC");
        cmd.add("-shared");
        cmd.add("-s");                          // Strip symbol table and debug info
        cmd.add("-fvisibility=hidden");         // Hide symbols

        // JNI headers
        if (javaHome != null) {
            File includeDir = new File(javaHome, "include");
            if (includeDir.exists()) {
                cmd.add("-I" + includeDir.getAbsolutePath());

                // Add all platform subdirs (cross-compilation needs target platform headers)
                String[] subDirs = {"win32", "linux", "darwin"};
                for (String sub : subDirs) {
                    File subDir = new File(includeDir, sub);
                    if (subDir.exists()) {
                        cmd.add("-I" + subDir.getAbsolutePath());
                    }
                }
            }
        }

        // Source files
        String[] sources = {"vm_data.c", "vm_interpreter.c", "vm_bridge.c", "chacha20.c"};
        for (String src : sources) {
            cmd.add(src);
        }

        cmd.add("-o");
        cmd.add(outputFile.getAbsolutePath());

        // Windows target does not need -lc, others do
        if (!target.contains("windows")) {
            cmd.add("-lc");
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(nativeDir);
        pb.redirectErrorStream(true);

        System.out.println("  [CMD] " + String.join(" ", cmd));

        Process proc = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                System.out.println("  [ZIG] " + line);
            }
        }

        boolean finished = proc.waitFor(180, TimeUnit.SECONDS);
        if (!finished) {
            proc.destroyForcibly();
            throw new RuntimeException("Zig compilation timed out for target: " + target);
        }

        int exitCode = proc.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException(
                    "Zig compilation failed for target: " + target + "\n" + output.toString());
        }

        if (outputFile.exists()) {
            outputLibraries.add(outputFile);
            System.out.println("  [OK] " + outputFile.getAbsolutePath() +
                    " (" + (outputFile.length() / 1024) + " KB)");
        } else {
            throw new RuntimeException("Output library not found: " + outputFile);
        }
    }

    private String findJavaHome() {
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isEmpty()) {
            // Confirm include directory exists
            if (new File(javaHome, "include").exists()) {
                return javaHome;
            }
        }

        // Infer from java.home
        String javaHomeProp = System.getProperty("java.home");
        if (javaHomeProp != null) {
            File jh = new File(javaHomeProp);
            // JDK 11+: java.home is directly JDK root
            if (new File(jh, "include").exists()) {
                return jh.getAbsolutePath();
            }
            // JDK 8: java.home is jre subdirectory
            File parent = jh.getParentFile();
            if (parent != null && new File(parent, "include").exists()) {
                return parent.getAbsolutePath();
            }
        }

        throw new RuntimeException(
                "Cannot find JDK. Set JAVA_HOME environment variable to your JDK installation.");
    }

    private void checkZigInstalled() throws Exception {
        try {
            ProcessBuilder pb = new ProcessBuilder("zig", "version");
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            StringBuilder out = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    out.append(line);
                }
            }

            proc.waitFor(10, TimeUnit.SECONDS);
            System.out.println("[ZIG] Zig version: " + out.toString().trim());

        } catch (IOException e) {
            throw new RuntimeException(
                    "Zig compiler not found. Please install Zig and add it to PATH.\n" +
                            "Download: https://ziglang.org/download/");
        }
    }

    public List<File> getOutputLibraries() {
        return outputLibraries;
    }
}