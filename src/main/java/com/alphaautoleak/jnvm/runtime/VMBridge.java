package com.alphaautoleak.jnvm.runtime;

import java.io.*;
import java.nio.file.*;

/**
 * Runtime bridge class - embedded in protected JAR.
 * Auto-detects system architecture, extracts native library from JAR to temp dir, then System.load.
 */
public final class VMBridge {

    private static final String LIB_NAME = "customvm";
    private static volatile boolean loaded = false;

    static {
        loadNativeLibrary();
    }

    /**
     * Global single native entry point
     */
    public static native Object execute(int methodId, Object instance, Object[] args);

    private VMBridge() {
        throw new UnsupportedOperationException();
    }

    /**
     * Loads native library:
     * 1. First try System.loadLibrary (if user placed dll/so)
     * 2. If failed, extract from JAR META-INF/native/<target>/ to temp dir
     */
    private static synchronized void loadNativeLibrary() {
        if (loaded) return;

        // Method 1: direct loadLibrary
        try {
            System.loadLibrary(LIB_NAME);
            loaded = true;
            return;
        } catch (UnsatisfiedLinkError e) {
            // Continue to try extracting from JAR
        }

        // Method 2: extract from JAR
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

            // Create temp directory
            Path tempDir = Files.createTempDirectory("jnvm-native-");
            tempDir.toFile().deleteOnExit();

            // Extract to temp file
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

            // System.load absolute path
            System.load(tempLib.toAbsolutePath().toString());
            loaded = true;

        } catch (IOException e) {
            throw new UnsatisfiedLinkError(
                    "Failed to extract native library: " + e.getMessage());
        }
    }

    /**
     * Gets JAR resource path for current platform
     */
    private static String getNativeResourcePath() {
        String target = getTargetTriple();
        String fileName = getLibFileName();

        // Exact match
        String path = "META-INF/native/" + target + "/" + fileName;
        if (VMBridge.class.getClassLoader().getResource(path) != null) {
            return path;
        }

        // Fuzzy match: try different target variants
        String[] variants = getPlatformVariants();
        for (String variant : variants) {
            path = "META-INF/native/" + variant + "/" + fileName;
            if (VMBridge.class.getClassLoader().getResource(path) != null) {
                return path;
            }
        }

        // Default directory
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