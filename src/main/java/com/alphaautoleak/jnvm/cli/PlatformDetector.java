package com.alphaautoleak.jnvm.cli;

/**
 * Platform detection utility
 */
public class PlatformDetector {

    /**
     * Detects current platform, maps to Zig target triple
     */
    public static String detectCurrentTarget() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();

        String zigArch = detectArch(arch);
        String zigOs = detectOs(os);

        return zigArch + "-" + zigOs;
    }

    private static String detectArch(String arch) {
        if (arch.contains("amd64") || arch.contains("x86_64")) {
            return "x86_64";
        } else if (arch.contains("aarch64") || arch.contains("arm64")) {
            return "aarch64";
        } else if (arch.contains("x86") || arch.contains("i386") || arch.contains("i686")) {
            return "x86";
        }
        return "x86_64"; // fallback
    }

    private static String detectOs(String os) {
        if (os.contains("linux")) {
            return "linux-gnu";
        } else if (os.contains("windows")) {
            return "windows-gnu";
        } else if (os.contains("mac") || os.contains("darwin")) {
            return "macos";
        }
        return "linux-gnu"; // fallback
    }
}
