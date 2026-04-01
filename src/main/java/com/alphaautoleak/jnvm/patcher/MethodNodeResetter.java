package com.alphaautoleak.jnvm.patcher;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

final class MethodNodeResetter {

    private MethodNodeResetter() {
    }

    static void markAsNative(MethodNode methodNode) {
        methodNode.access |= Opcodes.ACC_NATIVE;
        methodNode.access &= ~Opcodes.ACC_ABSTRACT;
        clear(methodNode);
    }

    static void replaceBody(MethodNode methodNode, InsnList instructions) {
        clear(methodNode);
        methodNode.instructions.add(instructions);
    }

    static void clear(MethodNode methodNode) {
        if (methodNode.instructions != null) {
            methodNode.instructions.clear();
        }
        methodNode.tryCatchBlocks.clear();
        if (methodNode.localVariables != null) {
            methodNode.localVariables.clear();
        }
        methodNode.maxStack = 0;
        methodNode.maxLocals = 0;
    }
}
