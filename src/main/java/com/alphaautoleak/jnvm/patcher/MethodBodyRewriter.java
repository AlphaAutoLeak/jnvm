package com.alphaautoleak.jnvm.patcher;

import com.alphaautoleak.jnvm.utils.InstructionsUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/**
 * Rewrites method body to call corresponding VMBridge.executeXxx() method
 * Selects different native functions based on return type, avoiding boxing/unboxing
 */
public class MethodBodyRewriter {

    private final String bridgeClass;
    private final int methodIdXorKey;

    // Execute method descriptors for each return type
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

        // 1. Push methodId (XOR obfuscated)
        int obfuscatedMethodId = methodId ^ methodIdXorKey;
        insns.add(new LdcInsnNode(obfuscatedMethodId));

        // 2. Push this or null
        if (isStatic) {
            insns.add(new InsnNode(Opcodes.ACONST_NULL));
        } else {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }

        // 3. Create parameter array Object[]
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

        // 4. Push caller class
        insns.add(new LdcInsnNode(org.objectweb.asm.Type.getType("L" + cn.name + ";")));

        // 5. Call corresponding native method based on return type
        String executeMethod = getExecuteMethod(retType);
        String executeDesc = getExecuteDesc(retType);
        insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, bridgeClass, executeMethod, executeDesc, false));

        // 6. Generate return instruction (no unboxing needed)
        generateDirectReturn(insns, retType);

        // Replace method body
        mn.instructions.clear();
        mn.instructions.add(insns);
        mn.tryCatchBlocks.clear();
        if (mn.localVariables != null) mn.localVariables.clear();
        mn.maxStack = 0;
        mn.maxLocals = 0;
    }

    /**
     * Gets execute method name based on return type
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
     * Gets method descriptor based on return type
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
     * Generates direct return instruction (no unboxing needed)
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