package com.alphaautoleak.jnvm.patcher;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

final class ReturnInsnAppender {

    private ReturnInsnAppender() {
    }

    static void appendDirectReturn(InsnList insns, Type retType) {
        switch (retType.getSort()) {
            case Type.VOID:
                insns.add(new InsnNode(Opcodes.RETURN));
                break;
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                insns.add(new InsnNode(Opcodes.IRETURN));
                break;
            case Type.LONG:
                insns.add(new InsnNode(Opcodes.LRETURN));
                break;
            case Type.FLOAT:
                insns.add(new InsnNode(Opcodes.FRETURN));
                break;
            case Type.DOUBLE:
                insns.add(new InsnNode(Opcodes.DRETURN));
                break;
            case Type.OBJECT:
            case Type.ARRAY:
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST, retType.getInternalName()));
                insns.add(new InsnNode(Opcodes.ARETURN));
                break;
            default:
                insns.add(new InsnNode(Opcodes.ARETURN));
                break;
        }
    }

    static void appendRawReturn(InsnList insns, Type retType) {
        switch (retType.getSort()) {
            case Type.VOID:
                insns.add(new InsnNode(Opcodes.RETURN));
                return;
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                insns.add(new InsnNode(Opcodes.IRETURN));
                return;
            case Type.LONG:
                insns.add(new InsnNode(Opcodes.LRETURN));
                return;
            case Type.FLOAT:
                insns.add(new InsnNode(Opcodes.FRETURN));
                return;
            case Type.DOUBLE:
                insns.add(new InsnNode(Opcodes.DRETURN));
                return;
            default:
                insns.add(new InsnNode(Opcodes.ARETURN));
        }
    }
}
