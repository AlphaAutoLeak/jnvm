package com.alphaautoleak.jnvm.utils;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class InstructionsUtil {

    public static void loadAndBox(InsnList insns, Type type, int localIdx) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
                insns.add(new VarInsnNode(Opcodes.ILOAD, localIdx));
                insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false));
                break;
            case Type.BYTE:
                insns.add(new VarInsnNode(Opcodes.ILOAD, localIdx));
                insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false));
                break;
            case Type.CHAR:
                insns.add(new VarInsnNode(Opcodes.ILOAD, localIdx));
                insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false));
                break;
            case Type.SHORT:
                insns.add(new VarInsnNode(Opcodes.ILOAD, localIdx));
                insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false));
                break;
            case Type.INT:
                insns.add(new VarInsnNode(Opcodes.ILOAD, localIdx));
                insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
                break;
            case Type.LONG:
                insns.add(new VarInsnNode(Opcodes.LLOAD, localIdx));
                insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false));
                break;
            case Type.FLOAT:
                insns.add(new VarInsnNode(Opcodes.FLOAD, localIdx));
                insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false));
                break;
            case Type.DOUBLE:
                insns.add(new VarInsnNode(Opcodes.DLOAD, localIdx));
                insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false));
                break;
            default:
                insns.add(new VarInsnNode(Opcodes.ALOAD, localIdx));
                break;
        }
    }

}
