package com.alphaautoleak.jnvm.patcher;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

final class ClinitRegistrationCoordinator {

    private final String bridgeClass;
    private final MethodBodyRewriter rewriter;

    ClinitRegistrationCoordinator(String bridgeClass, MethodBodyRewriter rewriter) {
        this.bridgeClass = bridgeClass;
        this.rewriter = rewriter;
    }

    void ensureClassNativeRegistration(ClassNode classNode) {
        MethodNode clinit = findClinit(classNode);
        if (clinit == null) {
            classNode.methods.add(rewriter.createSyntheticRegisterOnlyClinit(classNode.name));
            return;
        }
        if (!hasRegisterPreludeCall(clinit)) {
            rewriter.prependRegisterCallToClinit(classNode, clinit);
        }
    }

    private MethodNode findClinit(ClassNode classNode) {
        for (MethodNode method : classNode.methods) {
            if ("<clinit>".equals(method.name) && "()V".equals(method.desc)) {
                return method;
            }
        }
        return null;
    }

    private boolean hasRegisterPreludeCall(MethodNode clinit) {
        AbstractInsnNode instruction = clinit.instructions.getFirst();
        int inspected = 0;
        while (instruction != null && inspected < 16) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode methodInsn = (MethodInsnNode) instruction;
                if (methodInsn.getOpcode() == Opcodes.INVOKESTATIC
                        && bridgeClass.equals(methodInsn.owner)
                        && MethodBodyRewriter.REGISTER_NATIVE_METHOD_NAME.equals(methodInsn.name)
                        && MethodBodyRewriter.REGISTER_NATIVE_METHOD_DESC.equals(methodInsn.desc)) {
                    return true;
                }
            }
            instruction = instruction.getNext();
            inspected++;
        }
        return false;
    }
}
