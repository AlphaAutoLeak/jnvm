package com.alphaautoleak.jnvm.patcher;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class BridgeRuntimeBootstrapWriter {

    private final String bridgeClass;

    BridgeRuntimeBootstrapWriter(String bridgeClass) {
        this.bridgeClass = bridgeClass;
    }

    void write(ClassWriter cw) {
        writeClinit(cw);
        writeExtractAndLoadMethod(cw);
    }

    private void writeClinit(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();

        Label tryStart = new Label();
        Label tryEnd = new Label();
        Label catchHandler = new Label();
        Label end = new Label();

        mv.visitTryCatchBlock(tryStart, tryEnd, catchHandler, "java/lang/UnsatisfiedLinkError");
        mv.visitLabel(tryStart);
        mv.visitLdcInsn("customvm");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "loadLibrary", "(Ljava/lang/String;)V", false);
        mv.visitLabel(tryEnd);
        mv.visitJumpInsn(Opcodes.GOTO, end);

        mv.visitLabel(catchHandler);
        mv.visitVarInsn(Opcodes.ASTORE, 0);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, bridgeClass, "extractAndLoad", "()V", false);

        mv.visitLabel(end);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private void writeExtractAndLoadMethod(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "extractAndLoad", "()V", null, null);
        mv.visitCode();

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, bridgeClass, "detectTarget", "()Ljava/lang/String;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 0);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, bridgeClass, "detectLibName", "()Ljava/lang/String;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 1);

        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("META-INF/native/");
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitLdcInsn("/");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 2);

        mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(bridgeClass));
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/ClassLoader", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 3);

        mv.visitVarInsn(Opcodes.ALOAD, 3);
        Label notNull = new Label();
        mv.visitJumpInsn(Opcodes.IFNONNULL, notNull);
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/UnsatisfiedLinkError");
        mv.visitInsn(Opcodes.DUP);
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("JNVM native library not found in JAR: ");
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/UnsatisfiedLinkError", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(Opcodes.ATHROW);

        mv.visitLabel(notNull);
        mv.visitLdcInsn("jnvm-");
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/nio/file/attribute/FileAttribute");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/file/Files", "createTempDirectory", "(Ljava/lang/String;[Ljava/nio/file/attribute/FileAttribute;)Ljava/nio/file/Path;", false);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/nio/file/Path", "toFile", "()Ljava/io/File;", true);
        mv.visitVarInsn(Opcodes.ASTORE, 4);

        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/File", "deleteOnExit", "()V", false);

        mv.visitTypeInsn(Opcodes.NEW, "java/io/File");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/io/File;Ljava/lang/String;)V", false);
        mv.visitVarInsn(Opcodes.ASTORE, 5);

        mv.visitVarInsn(Opcodes.ALOAD, 5);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/File", "deleteOnExit", "()V", false);

        mv.visitTypeInsn(Opcodes.NEW, "java/io/FileOutputStream");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ALOAD, 5);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/io/FileOutputStream", "<init>", "(Ljava/io/File;)V", false);
        mv.visitVarInsn(Opcodes.ASTORE, 6);

        mv.visitIntInsn(Opcodes.SIPUSH, 8192);
        mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
        mv.visitVarInsn(Opcodes.ASTORE, 7);

        Label loopStart = new Label();
        Label loopEnd = new Label();
        mv.visitLabel(loopStart);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitVarInsn(Opcodes.ALOAD, 7);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/InputStream", "read", "([B)I", false);
        mv.visitVarInsn(Opcodes.ISTORE, 8);
        mv.visitVarInsn(Opcodes.ILOAD, 8);
        mv.visitInsn(Opcodes.ICONST_M1);
        mv.visitJumpInsn(Opcodes.IF_ICMPEQ, loopEnd);

        mv.visitVarInsn(Opcodes.ALOAD, 6);
        mv.visitVarInsn(Opcodes.ALOAD, 7);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitVarInsn(Opcodes.ILOAD, 8);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/FileOutputStream", "write", "([BII)V", false);
        mv.visitJumpInsn(Opcodes.GOTO, loopStart);

        mv.visitLabel(loopEnd);
        mv.visitVarInsn(Opcodes.ALOAD, 6);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/FileOutputStream", "close", "()V", false);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/InputStream", "close", "()V", false);

        mv.visitVarInsn(Opcodes.ALOAD, 5);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/File", "getAbsolutePath", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "load", "(Ljava/lang/String;)V", false);

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(5, 9);
        mv.visitEnd();
    }
}
