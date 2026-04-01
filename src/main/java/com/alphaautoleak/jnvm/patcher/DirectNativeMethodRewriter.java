package com.alphaautoleak.jnvm.patcher;

import com.alphaautoleak.jnvm.utils.BridgeFastPathUtil;
import com.alphaautoleak.jnvm.utils.InstructionsUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

final class DirectNativeMethodRewriter {

    private final String bridgeClass;
    private final int methodIdXorKey;
    private final ClinitNativeRegistrationHelper clinitHelper;

    DirectNativeMethodRewriter(String bridgeClass, int methodIdXorKey, ClinitNativeRegistrationHelper clinitHelper) {
        this.bridgeClass = bridgeClass;
        this.methodIdXorKey = methodIdXorKey;
        this.clinitHelper = clinitHelper;
    }

    void rewrite(MethodNode mn) {
        MethodNodeResetter.markAsNative(mn);
    }

    void rewriteClinit(ClassNode cn, MethodNode mn, int methodId, boolean classHasDirectNativeMethods) {
        InsnList insns = new InsnList();
        int obfMethodId = BridgeFastPathUtil.obfuscateMethodId(methodId, methodIdXorKey);

        if (classHasDirectNativeMethods) {
            insns.add(clinitHelper.buildRegisterClassNativesPrelude(cn.name));
        }

        InstructionsUtil.pushInt(insns, obfMethodId);
        InstructionsUtil.pushInt(insns, 2);
        insns.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));

        insns.add(new InsnNode(Opcodes.DUP));
        InstructionsUtil.pushInt(insns, 0);
        insns.add(new InsnNode(Opcodes.ACONST_NULL));
        insns.add(new InsnNode(Opcodes.AASTORE));

        insns.add(new InsnNode(Opcodes.DUP));
        InstructionsUtil.pushInt(insns, 1);
        insns.add(new LdcInsnNode(org.objectweb.asm.Type.getType("L" + cn.name + ";")));
        insns.add(new InsnNode(Opcodes.AASTORE));

        insns.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                bridgeClass,
                "executeVoid",
                "(I[Ljava/lang/Object;)V",
                false
        ));
        insns.add(new InsnNode(Opcodes.RETURN));
        MethodNodeResetter.replaceBody(mn, insns);
    }
}
