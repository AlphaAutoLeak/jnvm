package com.alphaautoleak.jnvm.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class ZigCommandBuilder {

    private static final String[] JNI_PLATFORM_SUBDIRS = {"win32", "linux", "darwin"};
    private static final String[] SOURCE_FILES = {"vm_data.c", "vm_interpreter.c", "vm_bridge.c", "chacha20.c"};

    File buildOutputFile(String target, File nativeDir) {
        File outputDir = new File(nativeDir, "out-" + target);
        outputDir.mkdirs();
        return new File(outputDir, getOutputLibraryName(target));
    }

    List<String> buildCompileCommand(File zigExecutable, String target, String javaHome, File outputFile) {
        List<String> cmd = new ArrayList<>();
        cmd.add(zigExecutable.getAbsolutePath());
        cmd.add("cc");
        cmd.add("-target");
        cmd.add(target);

        cmd.add("-fno-sanitize=all");
        cmd.add("-fno-sanitize-trap=all");
        cmd.add("-O3");
        cmd.add("-ffast-math");
        cmd.add("-flto");
        cmd.add("-DNDEBUG");
        cmd.add("-std=c11");
        cmd.add("-fPIC");
        cmd.add("-shared");
        cmd.add("-s");
        cmd.add("-fvisibility=hidden");

        appendJniIncludeDirs(cmd, javaHome);
        for (String src : SOURCE_FILES) {
            cmd.add(src);
        }

        cmd.add("-o");
        cmd.add(outputFile.getAbsolutePath());

        if (!target.contains("windows")) {
            cmd.add("-lc");
        }
        return cmd;
    }

    private String getOutputLibraryName(String target) {
        if (target.contains("windows")) {
            return "customvm.dll";
        }
        if (target.contains("macos") || target.contains("darwin")) {
            return "libcustomvm.dylib";
        }
        return "libcustomvm.so";
    }

    private void appendJniIncludeDirs(List<String> cmd, String javaHome) {
        if (javaHome == null) {
            return;
        }
        File includeDir = new File(javaHome, "include");
        if (!includeDir.exists()) {
            return;
        }
        cmd.add("-I" + includeDir.getAbsolutePath());
        for (String subDirName : JNI_PLATFORM_SUBDIRS) {
            File subDir = new File(includeDir, subDirName);
            if (subDir.exists()) {
                cmd.add("-I" + subDir.getAbsolutePath());
            }
        }
    }
}
