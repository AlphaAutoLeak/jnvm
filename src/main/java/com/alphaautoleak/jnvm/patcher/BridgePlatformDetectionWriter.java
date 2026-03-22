package com.alphaautoleak.jnvm.patcher;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class BridgePlatformDetectionWriter {

    private BridgePlatformDetectionWriter() {
    }

    static void write(ClassWriter cw) {
        writeDetectTargetMethod(cw);
        writeDetectLibNameMethod(cw);
    }

    private static void writeDetectTargetMethod(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "detectTarget", "()Ljava/lang/String;", null, null);
        mv.visitCode();

        mv.visitLdcInsn("os.arch");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "getProperty", "(Ljava/lang/String;)Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toLowerCase", "()Ljava/lang/String;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 0);

        mv.visitLdcInsn("os.name");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "getProperty", "(Ljava/lang/String;)Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toLowerCase", "()Ljava/lang/String;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 1);

        mv.visitLdcInsn("x86_64");
        mv.visitVarInsn(Opcodes.ASTORE, 2);

        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn("amd64");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "contains", "(Ljava/lang/CharSequence;)Z", false);
        Label notAmd64 = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, notAmd64);
        mv.visitLdcInsn("x86_64");
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        Label archDone = new Label();
        mv.visitJumpInsn(Opcodes.GOTO, archDone);

        mv.visitLabel(notAmd64);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn("aarch64");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "contains", "(Ljava/lang/CharSequence;)Z", false);
        Label notAarch64 = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, notAarch64);
        mv.visitLdcInsn("aarch64");
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        mv.visitJumpInsn(Opcodes.GOTO, archDone);

        mv.visitLabel(notAarch64);
        mv.visitLabel(archDone);

        mv.visitLdcInsn("linux-gnu");
        mv.visitVarInsn(Opcodes.ASTORE, 3);

        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitLdcInsn("windows");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "contains", "(Ljava/lang/CharSequence;)Z", false);
        Label notWindows = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, notWindows);
        mv.visitLdcInsn("windows-gnu");
        mv.visitVarInsn(Opcodes.ASTORE, 3);
        Label osDone = new Label();
        mv.visitJumpInsn(Opcodes.GOTO, osDone);

        mv.visitLabel(notWindows);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitLdcInsn("mac");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "contains", "(Ljava/lang/CharSequence;)Z", false);
        Label notMac = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, notMac);
        mv.visitLdcInsn("macos");
        mv.visitVarInsn(Opcodes.ASTORE, 3);
        mv.visitJumpInsn(Opcodes.GOTO, osDone);

        mv.visitLabel(notMac);
        mv.visitLabel(osDone);

        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitLdcInsn("-");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(3, 4);
        mv.visitEnd();
    }

    private static void writeDetectLibNameMethod(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "detectLibName", "()Ljava/lang/String;", null, null);
        mv.visitCode();

        mv.visitLdcInsn("os.name");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "getProperty", "(Ljava/lang/String;)Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toLowerCase", "()Ljava/lang/String;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 0);

        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn("windows");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "contains", "(Ljava/lang/CharSequence;)Z", false);
        Label notWin = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, notWin);
        mv.visitLdcInsn("customvm.dll");
        mv.visitInsn(Opcodes.ARETURN);

        mv.visitLabel(notWin);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn("mac");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "contains", "(Ljava/lang/CharSequence;)Z", false);
        Label notMac = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, notMac);
        mv.visitLdcInsn("libcustomvm.dylib");
        mv.visitInsn(Opcodes.ARETURN);

        mv.visitLabel(notMac);
        mv.visitLdcInsn("libcustomvm.so");
        mv.visitInsn(Opcodes.ARETURN);

        mv.visitMaxs(2, 1);
        mv.visitEnd();
    }
}
