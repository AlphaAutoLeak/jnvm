package com.alphaautoleak.jnvm.asm;

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

    private final ProtectConfig config;
    private final OpcodeObfuscator opcodeObfuscator;

    /** Global method ID counter */
    private int nextMethodId = 0;

    /** All collected methods to be protected */
    private final List<MethodInfo> protectedMethods = new ArrayList<>();

    /** Records which classes contain protected methods (for patching) */
    private final Set<String> affectedClasses = new HashSet<>();

    private final BootstrapMethodGuard bootstrapGuard = new BootstrapMethodGuard();

    /** Annotation rule descriptor list */
    private final List<String> annotationDescs;
    private final boolean protectBootstrapPayload;

    public JarScanner(ProtectConfig config, OpcodeObfuscator opcodeObfuscator) {
        this.config = config;
        this.opcodeObfuscator = opcodeObfuscator;
        this.annotationDescs = config.getAnnotationRules();
        this.protectBootstrapPayload = config.isProtectBootstrapPayload();
    }
    
    /**
     * Scans JAR file, returns all method info to be protected
     */
    public List<MethodInfo> scan(File jarFile) throws IOException {
        System.out.println("[SCAN] Opening JAR: " + jarFile.getAbsolutePath());

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
                    System.err.println("[WARN] Failed to process: " + entryName + " - " + e.getMessage());
                }
            }
        }

        // Never protect bootstrap-sensitive methods referenced by invokedynamic.
        // Strategy:
        // - default: skip all methods in bootstrap owner classes (safest)
        // - payload mode: only skip bootstrap-sensitive closure
        filterOutBootstrapMethods();

        System.out.println("[SCAN] Found " + protectedMethods.size() + " methods to protect in "
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

        // Skip interfaces (no method body) and synthetic classes
        if ((cn.access & Opcodes.ACC_INTERFACE) != 0 &&
                (cn.access & Opcodes.ACC_ABSTRACT) != 0) {
            // Interfaces may have default methods, also need to check
        }

        // Check class-level annotations
        boolean classAnnotated = false;
        if (!annotationDescs.isEmpty() && cn.visibleAnnotations != null) {
            for (AnnotationNode ann : cn.visibleAnnotations) {
                if (annotationDescs.contains(ann.desc)) {
                    classAnnotated = true;
                    break;
                }
            }
        }

        for (MethodNode mn : cn.methods) {
            // Skip abstract and native methods (no bytecode)
            if ((mn.access & Opcodes.ACC_ABSTRACT) != 0 ||
                    (mn.access & Opcodes.ACC_NATIVE) != 0) {
                continue;
            }

            // Skip methods without instructions
            if (mn.instructions == null || mn.instructions.size() == 0) {
                continue;
            }

            // Skip constructors (<init>) - they need proper this reference initialization
            if (mn.name.equals("<init>")) {
                continue;
            }

            if (!shouldProtectMethod(className, classAnnotated, mn)) {
                continue;
            }

            // Collect method info
            MethodInfo info = extractMethodInfo(cn, mn);
            if (info != null) {
                protectedMethods.add(info);
                affectedClasses.add(className);
                System.out.println("  [+] " + info);
            }
        }
    }

    /**
     * Extracts complete method metadata from ASM MethodNode
     */
    private MethodInfo extractMethodInfo(ClassNode cn, MethodNode mn) {
        MethodInfo info = new MethodInfo();
        info.setMethodId(nextMethodId++);
        info.setOwner(cn.name);
        info.setName(mn.name);
        info.setDescriptor(mn.desc);
        info.setAccess(mn.access);
        info.setMaxStack(mn.maxStack);
        info.setMaxLocals(mn.maxLocals);
        info.setSignature(mn.signature);

        // ===== Extract bytecode + metadata (new format) =====
        BytecodeExtractor extractor = new BytecodeExtractor(cn, mn, opcodeObfuscator);
        extractor.extract();

        info.setBytecode(extractor.getBytecode());
        info.setMetadata(extractor.getMetadata());
        info.setPcToMetaIdx(extractor.getPcToMetaIdx());
        info.setStringPool(extractor.getStringPool());
        info.setExceptionTable(extractor.getExceptionTable());
        info.setBootstrapMethods(extractor.getBootstrapMethods());

        return info;
    }

    public Set<String> getAffectedClasses() {
        return affectedClasses;
    }

    private void filterOutBootstrapMethods() {
        BootstrapMethodGuard.FilterOutcome outcome = bootstrapGuard.filter(protectedMethods, protectBootstrapPayload);
        if (!outcome.isChanged()) {
            return;
        }

        protectedMethods.clear();
        protectedMethods.addAll(outcome.getFilteredMethods());

        affectedClasses.clear();
        int id = 0;
        for (MethodInfo info : protectedMethods) {
            info.setMethodId(id++);
            affectedClasses.add(info.getOwner());
        }
        nextMethodId = id;

        System.out.println("[SCAN] Skipped " + outcome.getTotalSkippedCount() +
                " bootstrap-sensitive methods used by invokedynamic." +
                " (mode=" + outcome.getModeName() +
                ", class-owner=" + outcome.getSkipByClassCount() +
                ", dependency=" + outcome.getSkipByDependencyCount() + ")");
    }

    private boolean shouldProtectMethod(String className, boolean classAnnotated, MethodNode mn) {
        if (config.shouldProtect(className, mn.name)) {
            return true;
        }
        if (classAnnotated) {
            return true;
        }
        if (annotationDescs.isEmpty() || mn.visibleAnnotations == null) {
            return false;
        }
        for (AnnotationNode ann : mn.visibleAnnotations) {
            if (annotationDescs.contains(ann.desc)) {
                return true;
            }
        }
        return false;
    }
}
