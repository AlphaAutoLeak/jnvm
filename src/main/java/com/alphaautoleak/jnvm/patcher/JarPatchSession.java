package com.alphaautoleak.jnvm.patcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

final class JarPatchSession {

    private final File inputJar;
    private final File outputJar;
    private final Set<String> affectedClasses;
    private final String bridgeClass;
    private final BridgeClassGenerator bridgeGenerator;
    private final ClassPatcher classPatcher;

    JarPatchSession(File inputJar,
                    File outputJar,
                    Set<String> affectedClasses,
                    String bridgeClass,
                    BridgeClassGenerator bridgeGenerator,
                    ClassPatcher classPatcher) {
        this.inputJar = inputJar;
        this.outputJar = outputJar;
        this.affectedClasses = affectedClasses;
        this.bridgeClass = bridgeClass;
        this.bridgeGenerator = bridgeGenerator;
        this.classPatcher = classPatcher;
    }

    int run() throws IOException {
        int patchedCount = 0;
        try (JarFile jar = new JarFile(inputJar);
             JarOutputStream jos = new JarOutputStream(Files.newOutputStream(outputJar.toPath()), jar.getManifest())) {
            Enumeration<JarEntry> entries = jar.entries();
            Set<String> written = new HashSet<>();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (isManifest(entry)) {
                    continue;
                }

                try (InputStream is = jar.getInputStream(entry)) {
                    if (shouldPatch(entry)) {
                        writePatchedClass(jos, entry, is);
                        patchedCount++;
                    } else {
                        writeOriginalEntry(jos, entry, is);
                    }
                    written.add(entry.getName());
                }
            }

            injectBridgeClassIfMissing(jos, written);
        }
        return patchedCount;
    }

    private boolean isManifest(JarEntry entry) {
        return entry.getName().equalsIgnoreCase("META-INF/MANIFEST.MF");
    }

    private boolean shouldPatch(JarEntry entry) {
        return entry.getName().endsWith(".class") && affectedClasses.contains(entry.getName().replace(".class", ""));
    }

    private void writePatchedClass(JarOutputStream jos, JarEntry entry, InputStream is) throws IOException {
        byte[] original = JarStreams.readAll(is);
        byte[] patched = classPatcher.patch(original);
        jos.putNextEntry(new JarEntry(entry.getName()));
        jos.write(patched);
        jos.closeEntry();
    }

    private void writeOriginalEntry(JarOutputStream jos, JarEntry entry, InputStream is) throws IOException {
        jos.putNextEntry(new JarEntry(entry.getName()));
        if (!entry.isDirectory()) {
            JarStreams.copy(is, jos);
        }
        jos.closeEntry();
    }

    private void injectBridgeClassIfMissing(JarOutputStream jos, Set<String> written) throws IOException {
        String bridgePath = bridgeClass + ".class";
        if (written.contains(bridgePath)) {
            return;
        }

        jos.putNextEntry(new JarEntry(bridgePath));
        jos.write(bridgeGenerator.generate());
        jos.closeEntry();
        System.out.println("[PATCH] Injected " + bridgeClass.replace('/', '.') + ".class");
    }
}
