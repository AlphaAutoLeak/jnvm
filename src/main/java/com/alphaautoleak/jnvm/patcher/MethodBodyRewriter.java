package com.alphaautoleak.jnvm.patcher;

import com.alphaautoleak.jnvm.utils.InstructionsUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/**
 * 重写方法体为调用 VMBridge.execute()
 */
public class MethodBodyRewriter {

    private final String bridgeClass;
    private final int methodIdXorKey;

    /** execute 方法描述符 */
    private static final String EXECUTE_DESC =
            "(ILjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;";

    MethodBodyRewriter(String bridgeClass, int methodIdXorKey) {
        this.bridgeClass = bridgeClass;
        this.methodIdXorKey = methodIdXorKey;
    }

    void rewrite(ClassNode cn, MethodNode mn, int methodId) {
        boolean isStatic = (mn.access & Opcodes.ACC_STATIC) != 0;
        Type[] argTypes = Type.getArgumentTypes(mn.desc);
        Type retType = Type.getReturnType(mn.desc);

        InsnList insns = new InsnList();

        // 压入 methodId (XOR 混淆)
        int obfuscatedMethodId = methodId ^ methodIdXorKey;
        insns.add(new LdcInsnNode(obfuscatedMethodId));

        // 压入 this 或 null
        if (isStatic) {
            insns.add(new InsnNode(Opcodes.ACONST_NULL));
        } else {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }

        // 创建参数数组 Object[]
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

        // 4. 压入调用者类的 Class 对象（用于类加载器一致性）
        insns.add(new LdcInsnNode(org.objectweb.asm.Type.getType("L" + cn.name + ";")));

        // 5. 调用 VMBridge.execute(methodId, instance, args, callerClass)
        insns.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                bridgeClass,
                "execute",
                EXECUTE_DESC,
                false));

        //  处理返回值
        InstructionsUtil.generateReturn(insns, retType);

        // 替换方法体
        mn.instructions.clear();
        mn.instructions.add(insns);
        mn.tryCatchBlocks.clear();
        if (mn.localVariables != null) mn.localVariables.clear();
        mn.maxStack = 0;
        mn.maxLocals = 0;
    }

}
