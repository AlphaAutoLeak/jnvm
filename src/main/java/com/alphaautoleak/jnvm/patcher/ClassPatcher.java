package com.alphaautoleak.jnvm.patcher;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class ClassPatcher {

    private final String bridgeClass;
    private final boolean directNativeRewrite;
    private final Set<String> bootstrapMethodKeys;
    private final MethodPatchRegistry patchRegistry;
    private final MethodBodyRewriter rewriter;

    ClassPatcher(String bridgeClass,
                 boolean directNativeRewrite,
                 Set<String> bootstrapMethodKeys,
                 MethodPatchRegistry patchRegistry,
                 MethodBodyRewriter rewriter) {
        this.bridgeClass = bridgeClass;
        this.directNativeRewrite = directNativeRewrite;
        this.bootstrapMethodKeys = bootstrapMethodKeys;
        this.patchRegistry = patchRegistry;
        this.rewriter = rewriter;
    }

    byte[] patch(byte[] classBytes) {
        ClassReader cr = new ClassReader(classBytes);
        ClassNode cn = new ClassNode(Opcodes.ASM9);
        cr.accept(cn, 0);

        boolean classHasDirectNativeMethods = directNativeRewrite && patchRegistry.hasDirectNativeMethods(cn.name);
        List<MethodNode> appendedMethods = rewriteMethods(cn, classHasDirectNativeMethods);
        if (!appendedMethods.isEmpty()) {
            cn.methods.addAll(appendedMethods);
        }

        if (directNativeRewrite && classHasDirectNativeMethods && !patchRegistry.hasProtectedClinit(cn.name)) {
            ensureClinitRegistersClassNatives(cn);
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private List<MethodNode> rewriteMethods(ClassNode cn, boolean classHasDirectNativeMethods) {
        List<MethodNode> appendedMethods = new ArrayList<>();
        for (MethodNode mn : cn.methods) {
            Integer methodId = patchRegistry.findMethodId(cn.name, mn.name, mn.desc);
            if (methodId == null) {
                continue;
            }

            if (isBootstrapEntryMethod(cn.name, mn)) {
                MethodNode impl = rewriter.rewriteBootstrapEntryAsTrampoline(cn, mn, methodId);
                appendedMethods.add(impl);
                continue;
            }

            rewriter.rewrite(cn, mn, methodId, classHasDirectNativeMethods);
        }
        return appendedMethods;
    }

    private boolean isBootstrapEntryMethod(String owner, MethodNode mn) {
        if ("<clinit>".equals(mn.name) || "<init>".equals(mn.name)) {
            return false;
        }
        return bootstrapMethodKeys.contains(owner + "#" + mn.name + mn.desc)
                || bootstrapMethodKeys.contains(owner + "." + mn.name + mn.desc)
                || bootstrapMethodKeys.contains(com.alphaautoleak.jnvm.utils.MethodKeyUtil.of(owner, mn.name, mn.desc));
    }

    private void ensureClinitRegistersClassNatives(ClassNode cn) {
        MethodNode clinit = findClinit(cn);
        if (clinit == null) {
            cn.methods.add(rewriter.createSyntheticRegisterOnlyClinit(cn.name));
            return;
        }
        if (!hasRegisterPreludeCall(clinit)) {
            rewriter.prependRegisterCallToClinit(cn, clinit);
        }
    }

    private MethodNode findClinit(ClassNode cn) {
        for (MethodNode method : cn.methods) {
            if ("<clinit>".equals(method.name) && "()V".equals(method.desc)) {
                return method;
            }
        }
        return null;
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
}
