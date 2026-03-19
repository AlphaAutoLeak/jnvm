package com.alphaautoleak.jnvm.patcher;

import com.alphaautoleak.jnvm.asm.MethodInfo;
import com.alphaautoleak.jnvm.utils.BridgePackageNameGenerator;
import com.alphaautoleak.jnvm.utils.MethodKeyUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.*;

/**
 * Rewrites protected method bodies to call VMBridge.execute()
 */
public class JarPatcher {

    private final Set<String> affectedClasses;
    private final String bridgeClass;
    private final int methodIdXorKey;
    private final boolean directNativeRewrite;
    private final Map<String, Integer> methodIdMap = new HashMap<>();
    private final Set<String> classesWithDirectNativeMethods = new HashSet<>();
    private final Set<String> classesWithProtectedClinit = new HashSet<>();

    private final MethodBodyRewriter rewriter;
    private final BridgeClassGenerator bridgeGenerator;

    public JarPatcher(List<MethodInfo> protectedMethods,
                      Set<String> affectedClasses,
                      boolean directNativeRewrite) {
        this.affectedClasses = affectedClasses;
        this.bridgeClass = BridgePackageNameGenerator.generate();
        this.directNativeRewrite = directNativeRewrite;

        Random rand = new Random();
        int key;
        do {
            key = rand.nextInt();
        } while (key == 0);
        this.methodIdXorKey = key;

        for (MethodInfo m : protectedMethods) {
            String k = MethodKeyUtil.of(m.getOwner(), m.getName(), m.getDescriptor());
            methodIdMap.put(k, m.getMethodId());
            if (directNativeRewrite) {
                if (m.isClassInit()) {
                    classesWithProtectedClinit.add(m.getOwner());
                } else if (!m.isConstructor()) {
                    classesWithDirectNativeMethods.add(m.getOwner());
                }
            }
        }

        this.rewriter = new MethodBodyRewriter(bridgeClass, methodIdXorKey, directNativeRewrite);
        this.bridgeGenerator = new BridgeClassGenerator(bridgeClass);
    }

    public String getBridgeClass() {
        return bridgeClass;
    }

    public int getMethodIdXorKey() {
        return methodIdXorKey;
    }

    public void patch(File inputJar, File outputJar) throws IOException {
        System.out.println("[PATCH] Input:  " + inputJar);
        System.out.println("[PATCH] Output: " + outputJar);
        System.out.println("[PATCH] Bridge class: " + bridgeClass.replace('/', '.'));

        int patchedCount = 0;
        try (JarFile jar = new JarFile(inputJar);
             JarOutputStream jos = new JarOutputStream(
                     Files.newOutputStream(outputJar.toPath()), jar.getManifest())) {

            Enumeration<JarEntry> entries = jar.entries();
            Set<String> written = new HashSet<>();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                if (entry.getName().equalsIgnoreCase("META-INF/MANIFEST.MF")) {
                    continue;
                }

                try (InputStream is = jar.getInputStream(entry)) {
                    if (entry.getName().endsWith(".class") && isAffected(entry.getName())) {
                        byte[] original = readAll(is);
                        byte[] patched = patchClass(original);

                        jos.putNextEntry(new JarEntry(entry.getName()));
                        jos.write(patched);
                        jos.closeEntry();
                        patchedCount++;
                    } else {
                        jos.putNextEntry(new JarEntry(entry.getName()));
                        if (!entry.isDirectory()) {
                            copyStream(is, jos);
                        }
                        jos.closeEntry();
                    }
                    written.add(entry.getName());
                }
            }

            // Inject VMBridge.class
            String bridgePath = bridgeClass + ".class";
            if (!written.contains(bridgePath)) {
                jos.putNextEntry(new JarEntry(bridgePath));
                jos.write(bridgeGenerator.generate());
                jos.closeEntry();
                System.out.println("[PATCH] Injected " + bridgeClass.replace('/', '.') + ".class");
            }
        }

        System.out.println("[PATCH] Patched " + patchedCount + " classes.");
    }

    private boolean isAffected(String entryName) {
        String className = entryName.replace(".class", "");
        return affectedClasses.contains(className);
    }

    private byte[] patchClass(byte[] classBytes) {
        ClassReader cr = new ClassReader(classBytes);
        ClassNode cn = new ClassNode(Opcodes.ASM9);
        cr.accept(cn, 0);
        boolean classHasDirectNativeMethods = directNativeRewrite && classesWithDirectNativeMethods.contains(cn.name);

        for (MethodNode mn : cn.methods) {
            String key = MethodKeyUtil.of(cn.name, mn.name, mn.desc);
            Integer methodId = methodIdMap.get(key);
            if (methodId == null) continue;

            rewriter.rewrite(cn, mn, methodId, classHasDirectNativeMethods);
        }

        if (directNativeRewrite && classHasDirectNativeMethods
                && !classesWithProtectedClinit.contains(cn.name)) {
            ensureClinitRegistersClassNatives(cn);
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private void ensureClinitRegistersClassNatives(ClassNode cn) {
        MethodNode clinit = null;
        for (MethodNode method : cn.methods) {
            if ("<clinit>".equals(method.name) && "()V".equals(method.desc)) {
                clinit = method;
                break;
            }
        }
        if (clinit == null) {
            cn.methods.add(rewriter.createSyntheticRegisterOnlyClinit(cn.name));
            return;
        }
        if (hasRegisterPreludeCall(clinit)) {
            return;
        }
        rewriter.prependRegisterCallToClinit(cn, clinit);
    }

    private boolean hasRegisterPreludeCall(MethodNode clinit) {
        AbstractInsnNode cur = clinit.instructions.getFirst();
        int inspected = 0;
        while (cur != null && inspected < 16) {
            if (cur instanceof MethodInsnNode) {
                MethodInsnNode mi = (MethodInsnNode) cur;
                if (mi.getOpcode() == Opcodes.INVOKESTATIC
                        && bridgeClass.equals(mi.owner)
                        && MethodBodyRewriter.REGISTER_NATIVE_METHOD_NAME.equals(mi.name)
                        && MethodBodyRewriter.REGISTER_NATIVE_METHOD_DESC.equals(mi.desc)) {
                    return true;
                }
            }
            cur = cur.getNext();
            inspected++;
        }
        return false;
    }

    private byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) {
            baos.write(buf, 0, n);
        }
        return baos.toByteArray();
    }

    private void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
    }
}
