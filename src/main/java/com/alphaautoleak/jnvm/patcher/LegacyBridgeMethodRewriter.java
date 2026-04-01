package com.alphaautoleak.jnvm.patcher;

import com.alphaautoleak.jnvm.utils.BridgeFastPathUtil;
import com.alphaautoleak.jnvm.utils.InstructionsUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

final class LegacyBridgeMethodRewriter {

    private final String bridgeClass;
    private final int methodIdXorKey;

    LegacyBridgeMethodRewriter(String bridgeClass, int methodIdXorKey) {
        this.bridgeClass = bridgeClass;
        this.methodIdXorKey = methodIdXorKey;
    }

    void rewrite(ClassNode cn, MethodNode mn, int methodId) {
        boolean isStatic = (mn.access & Opcodes.ACC_STATIC) != 0;
        Type[] argTypes = Type.getArgumentTypes(mn.desc);
        Type retType = Type.getReturnType(mn.desc);

        InsnList insns = new InsnList();
        int obfMethodId = BridgeFastPathUtil.obfuscateMethodId(methodId, methodIdXorKey);

        InstructionsUtil.pushInt(insns, obfMethodId);

        int argsArrayLen = argTypes.length + 2;
        InstructionsUtil.pushInt(insns, argsArrayLen);
        insns.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));

        insns.add(new InsnNode(Opcodes.DUP));
        InstructionsUtil.pushInt(insns, 0);
        if (isStatic) {
            insns.add(new InsnNode(Opcodes.ACONST_NULL));
        } else {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        insns.add(new InsnNode(Opcodes.AASTORE));

        int localIdx = isStatic ? 0 : 1;
        for (int i = 0; i < argTypes.length; i++) {
            insns.add(new InsnNode(Opcodes.DUP));
            InstructionsUtil.pushInt(insns, i + 1);
            InstructionsUtil.loadAndBox(insns, argTypes[i], localIdx);
            insns.add(new InsnNode(Opcodes.AASTORE));
            localIdx += argTypes[i].getSize();
        }

        insns.add(new InsnNode(Opcodes.DUP));
        InstructionsUtil.pushInt(insns, argTypes.length + 1);
        insns.add(new LdcInsnNode(Type.getType("L" + cn.name + ";")));
        insns.add(new InsnNode(Opcodes.AASTORE));

        insns.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                bridgeClass,
                getLegacyExecuteMethodName(retType),
                getLegacyExecuteDesc(retType),
                false
        ));

        ReturnInsnAppender.appendDirectReturn(insns, retType);
        MethodNodeResetter.replaceBody(mn, insns);
    }

    private String getLegacyExecuteMethodName(Type retType) {
        switch (retType.getSort()) {
            case Type.VOID:
                return "executeVoid";
            case Type.LONG:
                return "executeLong";
            case Type.FLOAT:
                return "executeFloat";
            case Type.DOUBLE:
                return "executeDouble";
            case Type.OBJECT:
            case Type.ARRAY:
                return "executeObject";
            default:
                return "executeInt";
        }
    }

    private String getLegacyExecuteDesc(Type retType) {
        switch (retType.getSort()) {
            case Type.VOID:
                return "(I[Ljava/lang/Object;)V";
            case Type.LONG:
                return "(I[Ljava/lang/Object;)J";
            case Type.FLOAT:
                return "(I[Ljava/lang/Object;)F";
            case Type.DOUBLE:
                return "(I[Ljava/lang/Object;)D";
            case Type.OBJECT:
            case Type.ARRAY:
                return "(I[Ljava/lang/Object;)Ljava/lang/Object;";
            default:
                return "(I[Ljava/lang/Object;)I";
        }
    }
}
