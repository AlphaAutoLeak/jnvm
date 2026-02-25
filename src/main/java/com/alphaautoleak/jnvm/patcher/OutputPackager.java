package com.alphaautoleak.jnvm.patcher;

import java.io.*;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.*;

/**
 * 将编译好的 native 库嵌入到输出 JAR 中。
 */
public class OutputPackager {

    public void embedNativeLibraries(File jar, List<File> libraries) throws IOException {
        if (libraries.isEmpty()) {
            System.out.println("[PACKAGE] No native libraries to embed.");
            return;
        }

        File tempJar = new File(jar.getParent(), jar.getName() + ".tmp");

        try (JarFile original = new JarFile(jar);
             JarOutputStream jos = new JarOutputStream(
                     new FileOutputStream(tempJar), original.getManifest())) {

            Set<String> written = new HashSet<>();

            Enumeration<JarEntry> entries = original.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().equalsIgnoreCase("META-INF/MANIFEST.MF")) {
                    continue;
                }
                jos.putNextEntry(new JarEntry(entry.getName()));
                try (InputStream is = original.getInputStream(entry)) {
                    copyStream(is, jos);
                }
                jos.closeEntry();
                written.add(entry.getName());
            }

            // 添加 native 库
            for (File lib : libraries) {
                String target = inferTarget(lib);
                String entryName = "META-INF/native/" + target + "/" + lib.getName();

                if (written.contains(entryName)) continue;

                jos.putNextEntry(new JarEntry(entryName));
                try (FileInputStream fis = new FileInputStream(lib)) {
                    copyStream(fis, jos);
                }
                jos.closeEntry();
                written.add(entryName);

                System.out.println("[PACKAGE] Embedded: " + entryName +
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
        // zig-out/lib/ 情况
        if (parentName.equals("lib")) {
            return "default";
        }
        return "default";
    }

    private void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
    }
}