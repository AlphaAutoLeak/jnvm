package com.alphaautoleak.jnvm.compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class ZigEnvironment {

    private static final int ZIG_VERSION_TIMEOUT_SECONDS = 10;
    private static final String ZIG_FOLDER_PREFIX = "zig-";

    ZigInstallation resolveInstallation(File configuredExecutable) throws Exception {
        if (configuredExecutable != null) {
            return resolveConfiguredInstallation(configuredExecutable);
        }
        return resolveAdjacentInstallation();
    }

    ZigInstallation resolveInstallation() throws Exception {
        return resolveInstallation(null);
    }

    private ZigInstallation resolveAdjacentInstallation() throws Exception {
        File runtimeDirectory = findRuntimeDirectory();
        List<File> candidates = findZigDirectories(runtimeDirectory);
        if (candidates.isEmpty()) {
            throw new RuntimeException(
                    "Zig compiler not found next to the running JAR. "
                            + "Place an extracted Zig folder like 'zig-<arch>-<os>-<version>' "
                            + "beside the application and try again.");
        }

        File zigDirectory = selectBestCandidate(candidates);
        File executable = findExecutable(zigDirectory);
        if (executable == null) {
            throw new RuntimeException("Zig executable not found in: " + zigDirectory.getAbsolutePath());
        }

        return new ZigInstallation(zigDirectory, executable, readVersion(executable));
    }

    private ZigInstallation resolveConfiguredInstallation(File configuredExecutable) throws Exception {
        File executable = configuredExecutable.getAbsoluteFile();
        if (!executable.isFile()) {
            throw new RuntimeException("Configured zig executable not found: " + executable.getAbsolutePath());
        }

        File homeDirectory = executable.getParentFile();
        if (homeDirectory == null) {
            throw new RuntimeException("Configured zig executable has no parent directory: " + executable.getAbsolutePath());
        }

        return new ZigInstallation(homeDirectory, executable, readVersion(executable));
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

    private File findRuntimeDirectory() {
        CodeSource codeSource = ZigEnvironment.class.getProtectionDomain().getCodeSource();
        if (codeSource == null || codeSource.getLocation() == null) {
            return new File(System.getProperty("user.dir"));
        }

        try {
            File location = new File(codeSource.getLocation().toURI());
            if (location.isFile()) {
                File parent = location.getParentFile();
                if (parent != null) {
                    return parent;
                }
            }
        } catch (URISyntaxException ignored) {
        }

        return new File(System.getProperty("user.dir"));
    }

    private List<File> findZigDirectories(File runtimeDirectory) {
        File[] children = runtimeDirectory.listFiles();
        if (children == null || children.length == 0) {
            return Collections.emptyList();
        }

        List<File> candidates = new ArrayList<>();
        for (File child : children) {
            if (!child.isDirectory()) {
                continue;
            }
            if (!child.getName().toLowerCase().startsWith(ZIG_FOLDER_PREFIX)) {
                continue;
            }
            if (findExecutable(child) != null) {
                candidates.add(child);
            }
        }
        return candidates;
    }

    private File selectBestCandidate(List<File> candidates) {
        List<File> sorted = new ArrayList<>(candidates);
        Collections.sort(sorted, Comparator
                .comparingInt(this::candidateScore)
                .reversed()
                .thenComparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        return sorted.get(0);
    }

    private int candidateScore(File candidate) {
        String name = candidate.getName().toLowerCase();
        int score = 0;
        if (matchesHostOs(name)) {
            score += 2;
        }
        if (matchesHostArch(name)) {
            score += 2;
        }
        return score;
    }

    private boolean matchesHostOs(String candidateName) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return candidateName.contains("windows") || candidateName.contains("win");
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return candidateName.contains("macos") || candidateName.contains("darwin") || candidateName.contains("mac");
        }
        if (os.contains("linux")) {
            return candidateName.contains("linux");
        }
        return false;
    }

    private boolean matchesHostArch(String candidateName) {
        String arch = System.getProperty("os.arch", "").toLowerCase();
        if (arch.contains("64")) {
            if (arch.contains("aarch") || arch.contains("arm")) {
                return candidateName.contains("aarch64") || candidateName.contains("arm64");
            }
            return candidateName.contains("x86_64") || candidateName.contains("amd64") || candidateName.contains("x64");
        }
        if (arch.contains("86")) {
            return candidateName.contains("x86") || candidateName.contains("i386");
        }
        return candidateName.contains(arch);
    }

    private File findExecutable(File zigDirectory) {
        File windowsExe = new File(zigDirectory, "zig.exe");
        if (windowsExe.isFile()) {
            return windowsExe;
        }

        File unixExe = new File(zigDirectory, "zig");
        if (unixExe.isFile()) {
            return unixExe;
        }
        return null;
    }

    private String readVersion(File executable) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(executable.getAbsolutePath(), "version");
        processBuilder.directory(executable.getParentFile());
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        boolean finished = process.waitFor(ZIG_VERSION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Zig version check timed out: " + executable.getAbsolutePath());
        }
        if (process.exitValue() != 0) {
            throw new RuntimeException("Failed to query Zig version: " + executable.getAbsolutePath());
        }
        return output.toString().trim();
    }
}
