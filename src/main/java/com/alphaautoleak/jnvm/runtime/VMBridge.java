package com.alphaautoleak.jnvm.runtime;

import java.io.*;
import java.nio.file.*;

/**
 * 运行时桥接类 — 嵌入到 protected JAR 中。
 * 自动检测系统架构，从 JAR 中提取 native 库到临时目录，然后 System.load。
 */
public final class VMBridge {

    private static final String LIB_NAME = "customvm";
    private static volatile boolean loaded = false;

    static {
        loadNativeLibrary();
    }

    /**
     * 全局单一 native 入口
     */
    public static native Object execute(int methodId, Object instance, Object[] args);

    private VMBridge() {
        throw new UnsupportedOperationException();
    }

    /**
     * 加载 native 库：
     * 1. 先尝试 System.loadLibrary（如果用户自己放了 dll/so）
     * 2. 失败则从 JAR 中 META-INF/native/<target>/ 提取到临时目录
     */
    private static synchronized void loadNativeLibrary() {
        if (loaded) return;

        // 方式1: 直接 loadLibrary
        try {
            System.loadLibrary(LIB_NAME);
            loaded = true;
            return;
        } catch (UnsatisfiedLinkError e) {
            // 继续尝试从 JAR 提取
        }

        // 方式2: 从 JAR 提取
        try {
            String resourcePath = getNativeResourcePath();
            if (resourcePath == null) {
                throw new UnsatisfiedLinkError(
                        "No native library found for platform: " + getOsName() + "/" + getArch());
            }

            InputStream is = VMBridge.class.getClassLoader().getResourceAsStream(resourcePath);
            if (is == null) {
                throw new UnsatisfiedLinkError(
                        "Native library not found in JAR: " + resourcePath);
            }

            // 创建临时目录
            Path tempDir = Files.createTempDirectory("jnvm-native-");
            tempDir.toFile().deleteOnExit();

            // 提取到临时文件
            String fileName = getLibFileName();
            Path tempLib = tempDir.resolve(fileName);
            tempLib.toFile().deleteOnExit();

            try (OutputStream os = Files.newOutputStream(tempLib)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1) {
                    os.write(buf, 0, n);
                }
            }
            is.close();

            // System.load 绝对路径
            System.load(tempLib.toAbsolutePath().toString());
            loaded = true;

        } catch (IOException e) {
            throw new UnsatisfiedLinkError(
                    "Failed to extract native library: " + e.getMessage());
        }
    }

    /**
     * 获取当前平台对应的 JAR 内资源路径
     */
    private static String getNativeResourcePath() {
        String target = getTargetTriple();
        String fileName = getLibFileName();

        // 精确匹配
        String path = "META-INF/native/" + target + "/" + fileName;
        if (VMBridge.class.getClassLoader().getResource(path) != null) {
            return path;
        }

        // 模糊匹配：尝试不同的 target 变体
        String[] variants = getPlatformVariants();
        for (String variant : variants) {
            path = "META-INF/native/" + variant + "/" + fileName;
            if (VMBridge.class.getClassLoader().getResource(path) != null) {
                return path;
            }
        }

        // 默认目录
        path = "META-INF/native/default/" + fileName;
        if (VMBridge.class.getClassLoader().getResource(path) != null) {
            return path;
        }

        return null;
    }

    private static String getTargetTriple() {
        return getArch() + "-" + getOsTarget();
    }

    private static String getArch() {
        String arch = System.getProperty("os.arch", "").toLowerCase();
        if (arch.contains("amd64") || arch.contains("x86_64")) {
            return "x86_64";
        } else if (arch.contains("aarch64") || arch.contains("arm64")) {
            return "aarch64";
        } else if (arch.contains("arm")) {
            return "arm";
        } else if (arch.contains("x86") || arch.contains("i386") || arch.contains("i686")) {
            return "x86";
        }
        return arch;
    }

    private static String getOsName() {
        return System.getProperty("os.name", "").toLowerCase();
    }

    private static String getOsTarget() {
        String os = getOsName();
        if (os.contains("windows")) return "windows-gnu";
        if (os.contains("linux")) return "linux-gnu";
        if (os.contains("mac") || os.contains("darwin")) return "macos";
        return "linux-gnu";
    }

    private static String[] getPlatformVariants() {
        String arch = getArch();
        String os = getOsName();

        if (os.contains("windows")) {
            return new String[]{
                    arch + "-windows-gnu",
                    arch + "-windows-msvc",
                    arch + "-windows",
            };
        } else if (os.contains("linux")) {
            return new String[]{
                    arch + "-linux-gnu",
                    arch + "-linux-musl",
                    arch + "-linux",
            };
        } else if (os.contains("mac") || os.contains("darwin")) {
            return new String[]{
                    arch + "-macos",
                    arch + "-macos-none",
                    arch + "-darwin",
            };
        }
        return new String[]{arch + "-" + os};
    }

    private static String getLibFileName() {
        String os = getOsName();
        if (os.contains("windows")) {
            return LIB_NAME + ".dll";
        } else if (os.contains("mac") || os.contains("darwin")) {
            return "lib" + LIB_NAME + ".dylib";
        } else {
            return "lib" + LIB_NAME + ".so";
        }
    }
}