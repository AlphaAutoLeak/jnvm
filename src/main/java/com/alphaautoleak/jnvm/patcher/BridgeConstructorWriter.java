package com.alphaautoleak.jnvm.patcher;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class BridgeConstructorWriter {

    private BridgeConstructorWriter() {
    }

    static void writePrivateConstructor(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/UnsupportedOperationException");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/UnsupportedOperationException", "<init>", "()V", false);
        mv.visitInsn(Opcodes.ATHROW);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
    }
}
