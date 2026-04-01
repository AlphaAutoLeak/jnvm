package com.alphaautoleak.jnvm.patcher;

import com.alphaautoleak.jnvm.cli.CliReporter;

import java.io.*;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.*;

/**
 * Embeds compiled native library into output JAR.
 */
public class OutputPackager {

    public void embedNativeLibraries(File jar, List<File> libraries) throws IOException {
        if (libraries.isEmpty()) {
            CliReporter.tagged("PACKAGE", "No native libraries to embed.");
            return;
        }

        File tempJar = new File(jar.getParent(), jar.getName() + ".tmp");

        try (JarFile original = new JarFile(jar);
             JarOutputStream jos = new JarOutputStream(
                     Files.newOutputStream(tempJar.toPath()), original.getManifest())) {

            Set<String> written = new HashSet<>();

            Enumeration<JarEntry> entries = original.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().equalsIgnoreCase("META-INF/MANIFEST.MF")) {
                    continue;
                }
                jos.putNextEntry(new JarEntry(entry.getName()));
                try (InputStream is = original.getInputStream(entry)) {
                    JarStreams.copy(is, jos);
                }
                jos.closeEntry();
                written.add(entry.getName());
            }

            // Add native library
            for (File lib : libraries) {
                String target = inferTarget(lib);
                String entryName = "META-INF/native/" + target + "/" + lib.getName();

                if (written.contains(entryName)) continue;

                jos.putNextEntry(new JarEntry(entryName));
                try (FileInputStream fis = new FileInputStream(lib)) {
                    JarStreams.copy(fis, jos);
                }
                jos.closeEntry();
                written.add(entryName);

                CliReporter.tagged("PACKAGE", "Embedded: " + entryName +
                        " (" + (lib.length() / 1024) + " KB)");
            }
        }

        if (!jar.delete()) {
            throw new IOException("Cannot delete original jar: " + jar);
        }
        if (!tempJar.renameTo(jar)) {
            throw new IOException("Cannot rename temp jar to: " + jar);
        }
    }

    private String inferTarget(File lib) {
        String parentName = lib.getParentFile().getName();
        if (parentName.startsWith("out-")) {
            return parentName.substring(4);
        }
        // zig-out/lib/ case
        if (parentName.equals("lib")) {
            return "default";
        }
        return "default";
    }
}
