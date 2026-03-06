package com.alphaautoleak.jnvm.patcher;

import com.alphaautoleak.jnvm.utils.InstructionsUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/**
 * 重写方法体为调用对应的 VMBridge.executeXxx() 方法
 * 根据返回类型选择不同的 native 函数，避免装箱/拆箱
 */
public class MethodBodyRewriter {

    private final String bridgeClass;
    private final int methodIdXorKey;

    // 各返回类型对应的 execute 方法描述符
    private static final String EXECUTE_VOID_DESC   = "(ILjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Class;)V";
    private static final String EXECUTE_INT_DESC    = "(ILjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Class;)I";
    private static final String EXECUTE_LONG_DESC   = "(ILjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Class;)J";
    private static final String EXECUTE_FLOAT_DESC  = "(ILjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Class;)F";
    private static final String EXECUTE_DOUBLE_DESC = "(ILjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Class;)D";
    private static final String EXECUTE_OBJECT_DESC = "(ILjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;";

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
            InstructionsUtil.loadAndBox(insns, argTypes[i], localIdx);
            insns.add(new InsnNode(Opcodes.AASTORE));
            localIdx += argTypes[i].getSize();
        }

        // 4. 压入调用者类
        insns.add(new LdcInsnNode(org.objectweb.asm.Type.getType("L" + cn.name + ";")));

        // 5. 根据返回类型调用对应的 native 方法
        String executeMethod = getExecuteMethod(retType);
        String executeDesc = getExecuteDesc(retType);
        insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, bridgeClass, executeMethod, executeDesc, false));

        // 6. 生成返回指令（不再需要拆箱）
        generateDirectReturn(insns, retType);

        // 替换方法体
        mn.instructions.clear();
        mn.instructions.add(insns);
        mn.tryCatchBlocks.clear();
        if (mn.localVariables != null) mn.localVariables.clear();
        mn.maxStack = 0;
        mn.maxLocals = 0;
    }

    /**
     * 根据返回类型获取对应的 execute 方法名
     */
    private String getExecuteMethod(Type retType) {
        switch (retType.getSort()) {
            case Type.VOID:
                return "executeVoid";
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return "executeInt";
            case Type.LONG:
                return "executeLong";
            case Type.FLOAT:
                return "executeFloat";
            case Type.DOUBLE:
                return "executeDouble";
            default:
                return "executeObject";
        }
    }

    /**
     * 根据返回类型获取对应的方法描述符
     */
    private String getExecuteDesc(Type retType) {
        switch (retType.getSort()) {
            case Type.VOID:
                return EXECUTE_VOID_DESC;
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return EXECUTE_INT_DESC;
            case Type.LONG:
                return EXECUTE_LONG_DESC;
            case Type.FLOAT:
                return EXECUTE_FLOAT_DESC;
            case Type.DOUBLE:
                return EXECUTE_DOUBLE_DESC;
            default:
                return EXECUTE_OBJECT_DESC;
        }
    }

    /**
     * 生成直接的返回指令（不再需要拆箱）
     */
    private void generateDirectReturn(InsnList insns, Type retType) {
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
}