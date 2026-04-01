package com.alphaautoleak.jnvm.patcher;

import com.alphaautoleak.jnvm.utils.InstructionsUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

final class BootstrapTrampolineRewriter {

    private final LegacyBridgeMethodRewriter legacyRewriter;
    private int bootstrapImplSeq = 0;

    BootstrapTrampolineRewriter(LegacyBridgeMethodRewriter legacyRewriter) {
        this.legacyRewriter = legacyRewriter;
    }

    MethodNode rewrite(ClassNode cn, MethodNode bootstrapEntry, int methodId) {
        String implName = "__jnvm$bootstrap$impl$" + (bootstrapImplSeq++);
        MethodNode impl = cloneAsBootstrapImpl(bootstrapEntry, implName);
        legacyRewriter.rewrite(cn, impl, methodId);
        rewriteBootstrapTrampolineBody(cn.name, bootstrapEntry, implName);
        return impl;
    }

    private MethodNode cloneAsBootstrapImpl(MethodNode source, String implName) {
        int implAccess = (source.access & ~(Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED))
                | Opcodes.ACC_PRIVATE
                | Opcodes.ACC_SYNTHETIC;
        MethodNode impl = new MethodNode(
                Opcodes.ASM9,
                implAccess,
                implName,
                source.desc,
                source.signature,
                source.exceptions == null ? null : source.exceptions.toArray(new String[0])
        );
        source.accept(impl);
        return impl;
    }

    private void rewriteBootstrapTrampolineBody(String owner, MethodNode entry, String implName) {
        boolean isStatic = (entry.access & Opcodes.ACC_STATIC) != 0;
        Type[] argTypes = Type.getArgumentTypes(entry.desc);
        Type retType = Type.getReturnType(entry.desc);

        InsnList insns = new InsnList();
        int localIdx = 0;
        int invokeOpcode;
        if (isStatic) {
            invokeOpcode = Opcodes.INVOKESTATIC;
        } else {
            invokeOpcode = Opcodes.INVOKESPECIAL;
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            localIdx = 1;
        }

        for (Type argType : argTypes) {
            InstructionsUtil.loadRaw(insns, argType, localIdx);
            localIdx += argType.getSize();
        }

        insns.add(new MethodInsnNode(invokeOpcode, owner, implName, entry.desc, false));
        ReturnInsnAppender.appendRawReturn(insns, retType);
        MethodNodeResetter.replaceBody(entry, insns);
    }
}
