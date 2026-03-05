package com.alphaautoleak.jnvm.patcher;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/**
 * 重写方法体为调用 VMBridge.execute()
 */
class MethodBodyRewriter {

    private final String bridgeClass;
    private final int methodIdXorKey;

    /** execute 方法描述符 */
    private static final String EXECUTE_DESC =
            "(ILjava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;";

    MethodBodyRewriter(String bridgeClass, int methodIdXorKey) {
        this.bridgeClass = bridgeClass;
        this.methodIdXorKey = methodIdXorKey;
    }

    void rewrite(ClassNode cn, MethodNode mn, int methodId) {
        boolean isStatic = (mn.access & Opcodes.ACC_STATIC) != 0;
        Type[] argTypes = Type.getArgumentTypes(mn.desc);
        Type retType = Type.getReturnType(mn.desc);

        InsnList insns = new InsnList();

        // 1. 压入 methodId (XOR 混淆)
        int obfuscatedMethodId = methodId ^ methodIdXorKey;
        insns.add(new LdcInsnNode(obfuscatedMethodId));

        // 2. 压入 this 或 null
        if (isStatic) {
            insns.add(new InsnNode(Opcodes.ACONST_NULL));
        } else {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }

        // 3. 创建参数数组 Object[]
        insns.add(new LdcInsnNode(argTypes.length));
        insns.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));

        int localIdx = isStatic ? 0 : 1;
        for (int i = 0; i < argTypes.length; i++) {
            insns.add(new InsnNode(Opcodes.DUP));
            insns.add(new LdcInsnNode(i));
            loadAndBox(insns, argTypes[i], localIdx);
            insns.add(new InsnNode(Opcodes.AASTORE));
            localIdx += argTypes[i].getSize();
        }

        // 4. 调用 VMBridge.execute(methodId, instance, args)
        insns.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                bridgeClass,
                "execute",
                EXECUTE_DESC,
                false));

        // 5. 处理返回值
        generateReturn(insns, retType);

        // 替换方法体
        mn.instructions.clear();
        mn.instructions.add(insns);
        mn.tryCatchBlocks.clear();
        if (mn.localVariables != null) mn.localVariables.clear();
        mn.maxStack = 0;
        mn.maxLocals = 0;
    }

    private void loadAndBox(InsnList insns, Type type, int localIdx) {
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

    private void generateReturn(InsnList insns, Type retType) {
        switch (retType.getSort()) {
            case Type.VOID:
                insns.add(new InsnNode(Opcodes.POP));
                insns.add(new InsnNode(Opcodes.RETURN));
                break;
            case Type.BOOLEAN:
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Boolean"));
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                        "java/lang/Boolean", "booleanValue", "()Z", false));
                insns.add(new InsnNode(Opcodes.IRETURN));
                break;
            case Type.BYTE:
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Byte"));
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                        "java/lang/Byte", "byteValue", "()B", false));
                insns.add(new InsnNode(Opcodes.IRETURN));
                break;
            case Type.CHAR:
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Character"));
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                        "java/lang/Character", "charValue", "()C", false));
                insns.add(new InsnNode(Opcodes.IRETURN));
                break;
            case Type.SHORT:
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Short"));
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                        "java/lang/Short", "shortValue", "()S", false));
                insns.add(new InsnNode(Opcodes.IRETURN));
                break;
            case Type.INT:
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Integer"));
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                        "java/lang/Integer", "intValue", "()I", false));
                insns.add(new InsnNode(Opcodes.IRETURN));
                break;
            case Type.LONG:
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Long"));
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                        "java/lang/Long", "longValue", "()J", false));
                insns.add(new InsnNode(Opcodes.LRETURN));
                break;
            case Type.FLOAT:
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Float"));
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                        "java/lang/Float", "floatValue", "()F", false));
                insns.add(new InsnNode(Opcodes.FRETURN));
                break;
            case Type.DOUBLE:
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Double"));
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                        "java/lang/Double", "doubleValue", "()D", false));
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
}
