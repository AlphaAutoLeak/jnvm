package com.alphaautoleak.jnvm.asm;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

final class StackTraceSensitivityDetector {

    boolean isSensitive(ClassNode classNode) {
        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.instructions == null) {
                continue;
            }
            for (AbstractInsnNode instruction = methodNode.instructions.getFirst();
                 instruction != null;
                 instruction = instruction.getNext()) {
                if (!(instruction instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode methodInsn = (MethodInsnNode) instruction;
                if (isThrowableGetStackTrace(methodInsn) || isStackTraceElementGetMethodName(methodInsn)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isThrowableGetStackTrace(MethodInsnNode methodInsn) {
        return "java/lang/Throwable".equals(methodInsn.owner)
                && "getStackTrace".equals(methodInsn.name)
                && "()[Ljava/lang/StackTraceElement;".equals(methodInsn.desc);
    }

    private boolean isStackTraceElementGetMethodName(MethodInsnNode methodInsn) {
        return "java/lang/StackTraceElement".equals(methodInsn.owner)
                && "getMethodName".equals(methodInsn.name)
                && "()Ljava/lang/String;".equals(methodInsn.desc);
    }
}
