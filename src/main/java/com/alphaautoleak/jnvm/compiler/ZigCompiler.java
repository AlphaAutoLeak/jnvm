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
     * 直接用 zig cc 编译 — 最可靠，不依赖 build.zig API 版本
     */
    private void compileDirect(String target, String javaHome) throws Exception {
        File nativeDir = config.getNativeDir();

        // 输出文件名
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

        cmd.add("-fno-sanitize=all");           // 禁用所有运行时代码清洗/安全检查
        cmd.add("-fno-sanitize-trap=all");      // 防止编译器为未定义行为生成陷阱指令
        cmd.add("-Os");                         // 优化代码体积（替代 -O2）
        cmd.add("-fno-optimize-sibling-calls"); // 禁用兄弟调用优化
        cmd.add("-fno-slp-vectorize");          // 禁用 SLP 向量化

        cmd.add("-std=c11");
        cmd.add("-fPIC");
        cmd.add("-shared");
        cmd.add("-s");                          // 删除符号表和调试信息
        cmd.add("-fvisibility=hidden");         // 隐藏符号

        // JNI 头文件
        if (javaHome != null) {
            File includeDir = new File(javaHome, "include");
            if (includeDir.exists()) {
                cmd.add("-I" + includeDir.getAbsolutePath());

                // 所有平台子目录都加上（交叉编译时需要目标平台的头文件）
                String[] subDirs = {"win32", "linux", "darwin"};
                for (String sub : subDirs) {
                    File subDir = new File(includeDir, sub);
                    if (subDir.exists()) {
                        cmd.add("-I" + subDir.getAbsolutePath());
                    }
                }
            }
        }

        // 源文件
        String[] sources = {"vm_data.c", "vm_interpreter.c", "vm_bridge.c", "chacha20.c"};
        for (String src : sources) {
            cmd.add(src);
        }

        cmd.add("-o");
        cmd.add(outputFile.getAbsolutePath());

        // Windows 目标不需要 -lc，其他需要
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
            // 确认 include 目录存在
            if (new File(javaHome, "include").exists()) {
                return javaHome;
            }
        }

        // 从 java.home 推断
        String javaHomeProp = System.getProperty("java.home");
        if (javaHomeProp != null) {
            File jh = new File(javaHomeProp);
            // JDK 11+: java.home 直接是 JDK 根目录
            if (new File(jh, "include").exists()) {
                return jh.getAbsolutePath();
            }
            // JDK 8: java.home 是 jre 子目录
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