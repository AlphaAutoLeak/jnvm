package com.alphaautoleak.jnvm.patcher;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

final class ClinitNativeRegistrationHelper {

    private final String bridgeClass;

    ClinitNativeRegistrationHelper(String bridgeClass) {
        this.bridgeClass = bridgeClass;
    }

    void prependRegisterCallToClinit(ClassNode cn, MethodNode clinit) {
        clinit.instructions.insert(buildRegisterClassNativesPrelude(cn.name));
    }

    MethodNode createSyntheticRegisterOnlyClinit(String ownerClassInternalName) {
        MethodNode mn = new MethodNode(
                Opcodes.ACC_STATIC,
                "<clinit>",
                "()V",
                null,
                null
        );
        mn.instructions.add(buildRegisterClassNativesPrelude(ownerClassInternalName));
        mn.instructions.add(new InsnNode(Opcodes.RETURN));
        mn.maxStack = 0;
        mn.maxLocals = 0;
        return mn;
    }

    InsnList buildRegisterClassNativesPrelude(String ownerClassInternalName) {
        InsnList insns = new InsnList();
        insns.add(new LdcInsnNode(Type.getType("L" + ownerClassInternalName + ";")));
        insns.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                bridgeClass,
                MethodBodyRewriter.REGISTER_NATIVE_METHOD_NAME,
                MethodBodyRewriter.REGISTER_NATIVE_METHOD_DESC,
                false
        ));
        return insns;
    }
}
