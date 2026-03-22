package com.alphaautoleak.jnvm.compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class ZigProcessRunner {

    private static final int COMPILE_TIMEOUT_SECONDS = 180;

    String runCompileProcess(String target, File nativeDir, List<String> command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(nativeDir);
        pb.redirectErrorStream(true);

        System.out.println("  [CMD] " + String.join(" ", command));

        Process proc = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                System.out.println("  [ZIG] " + line);
            }
        }

        boolean finished = proc.waitFor(COMPILE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            proc.destroyForcibly();
            throw new RuntimeException("Zig compilation timed out for target: " + target);
        }

        int exitCode = proc.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Zig compilation failed for target: " + target + "\n" + output);
        }
        return output.toString();
    }
}
