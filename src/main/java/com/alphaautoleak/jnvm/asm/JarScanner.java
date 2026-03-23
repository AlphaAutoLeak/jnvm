package com.alphaautoleak.jnvm.asm;

import com.alphaautoleak.jnvm.cli.CliReporter;
import com.alphaautoleak.jnvm.config.ProtectConfig;
import com.alphaautoleak.jnvm.crypto.OpcodeObfuscator;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Scans input JAR, collects all method metadata to be protected.
 */
public class JarScanner {

    private final MethodProtectionDecider protectionDecider;
    private final MethodInfoFactory methodInfoFactory;
    private final StackTraceSensitivityDetector stackTraceSensitivityDetector = new StackTraceSensitivityDetector();

    /** Global method ID counter */
    private int nextMethodId = 0;

    /** All collected methods to be protected */
    private final List<MethodInfo> protectedMethods = new ArrayList<>();

    /** Records which classes contain protected methods (for patching) */
    private final Set<String> affectedClasses = new HashSet<>();

    private final BootstrapMethodGuard bootstrapGuard = new BootstrapMethodGuard();
    private Set<String> bootstrapMethodKeys = Collections.emptySet();

    public JarScanner(ProtectConfig config, OpcodeObfuscator opcodeObfuscator) {
        this.protectionDecider = new MethodProtectionDecider(config);
        this.methodInfoFactory = new MethodInfoFactory(opcodeObfuscator);
    }
    
    /**
     * Scans JAR file, returns all method info to be protected
     */
    public List<MethodInfo> scan(File jarFile) throws IOException {
        CliReporter.tagged("SCAN", "Opening JAR: " + jarFile.getAbsolutePath());

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                // Only process .class files
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }

                // Skip module-info and package-info
                String entryName = entry.getName();
                if (entryName.equals("module-info.class") ||
                        entryName.endsWith("package-info.class")) {
                    continue;
                }

                try (InputStream is = jar.getInputStream(entry)) {
                    processClass(is);
                } catch (Exception e) {
                    CliReporter.taggedError("WARN", "Failed to process: " + entryName + " - " + e.getMessage());
                }
            }
        }

        // Record real bootstrap entry methods. We no longer perform bootstrap-sensitive
        // skip filtering here; bootstrap entries are handled by trampoline rewrite later.
        bootstrapMethodKeys = bootstrapGuard.getBootstrapMethodTargetsSnapshot();
        if (!bootstrapMethodKeys.isEmpty()) {
            CliReporter.tagged("SCAN", "Detected " + bootstrapMethodKeys.size()
                    + " invokedynamic bootstrap entry methods.");
        }

        CliReporter.tagged("SCAN", "Found " + protectedMethods.size() + " methods to protect in "
                + affectedClasses.size() + " classes.");
        return protectedMethods;
    }

    /**
     * Process single class file
     */
    private void processClass(InputStream classBytes) throws IOException {
        ClassReader cr = new ClassReader(classBytes);
        ClassNode cn = new ClassNode(Opcodes.ASM9);
        cr.accept(cn, 0); // do not skip anything

        String className = cn.name; // internal format

        bootstrapGuard.scanClass(cn);
        boolean stackTraceSensitiveClass = stackTraceSensitivityDetector.isSensitive(cn);

        // Skip interfaces (no method body) and synthetic classes
        if ((cn.access & Opcodes.ACC_INTERFACE) != 0 &&
                (cn.access & Opcodes.ACC_ABSTRACT) != 0) {
            // Interfaces may have default methods, also need to check
        }

        // Check class-level annotations
        boolean classAnnotated = protectionDecider.isClassAnnotated(cn);

        for (MethodNode mn : cn.methods) {
            if (stackTraceSensitiveClass) {
                continue;
            }
            if (!protectionDecider.isMethodEligible(mn)) {
                continue;
            }

            if (!protectionDecider.shouldProtectMethod(className, classAnnotated, mn)) {
                continue;
            }

            // Collect method info
            MethodInfo info;
            try {
                info = methodInfoFactory.create(nextMethodId++, cn, mn);
            } catch (Exception ex) {
                CliReporter.warn("Extraction failed: "
                        + className.replace('/', '.') + "." + mn.name + mn.desc
                        + " - " + ex.getMessage());
                continue;
            }
            if (info != null) {
                protectedMethods.add(info);
                affectedClasses.add(className);
                CliReporter.verbose("  [+] " + info);
            }
        }

        if (stackTraceSensitiveClass) {
            CliReporter.taggedVerbose("SCAN", "Skipped stacktrace-sensitive class: " + className.replace('/', '.'));
        }
    }

    public Set<String> getAffectedClasses() {
        return affectedClasses;
    }

    public Set<String> getBootstrapMethodKeys() {
        return bootstrapMethodKeys;
    }
}
