package com.alphaautoleak.jnvm.patcher;

import org.objectweb.asm.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * 生成 VMBridge 类的字节码
 */
class BridgeClassGenerator {

    private final String bridgeClass;

    /** execute 方法描述符 */
    private static final String EXECUTE_DESC =
            "(ILjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;";

    BridgeClassGenerator(String bridgeClass) {
        this.bridgeClass = bridgeClass;
    }

    byte[] generate() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        cw.visit(Opcodes.V1_8,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                bridgeClass,
                null,
                "java/lang/Object",
                null);

        // static native Object execute(int, Object, Object[])
        // 注意: 虽然声明为 native，但实际由 RegisterNatives 注册
        // JVM 会查找通过 RegisterNatives 注册的方法实现
        cw.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_NATIVE,
                "execute",
                EXECUTE_DESC,
                null,
                null).visitEnd();

        generateClinit(cw);
        generateExtractAndLoadMethod(cw);
        generateDetectTargetMethod(cw);
        generateDetectLibNameMethod(cw);
        generatePrivateConstructor(cw);

        cw.visitEnd();
        return cw.toByteArray();
    }

    private void generateClinit(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_STATIC,
                "<clinit>",
                "()V",
                null,
                null);
        mv.visitCode();

        Label tryStart = new Label();
        Label tryEnd = new Label();
        Label catchHandler = new Label();
        Label end = new Label();

        mv.visitTryCatchBlock(tryStart, tryEnd, catchHandler, "java/lang/UnsatisfiedLinkError");

        // try { System.loadLibrary("customvm"); }
        mv.visitLabel(tryStart);
        mv.visitLdcInsn("customvm");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                "java/lang/System", "loadLibrary",
                "(Ljava/lang/String;)V", false);
        mv.visitLabel(tryEnd);
        mv.visitJumpInsn(Opcodes.GOTO, end);

        // catch (UnsatisfiedLinkError e) { extractAndLoad(); }
        mv.visitLabel(catchHandler);
        mv.visitVarInsn(Opcodes.ASTORE, 0);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                bridgeClass, "extractAndLoad", "()V", false);

        mv.visitLabel(end);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private void generatePrivateConstructor(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                "java/lang/Object", "<init>", "()V", false);
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/UnsupportedOperationException");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                "java/lang/UnsupportedOperationException", "<init>", "()V", false);
        mv.visitInsn(Opcodes.ATHROW);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
    }

    private void generateExtractAndLoadMethod(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                "extractAndLoad", "()V", null, null);
        mv.visitCode();

        // String target = detectTarget();
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, bridgeClass,
                "detectTarget", "()Ljava/lang/String;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 0);

        // String libName = detectLibName();
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, bridgeClass,
                "detectLibName", "()Ljava/lang/String;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 1);

        // String resPath = "META-INF/native/" + target + "/" + libName
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("META-INF/native/");
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder",
                "<init>", "(Ljava/lang/String;)V", false);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder",
                "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitLdcInsn("/");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder",
                "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder",
                "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder",
                "toString", "()Ljava/lang/String;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 2);

        // InputStream is = ...getResourceAsStream(resPath)
        mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(bridgeClass));
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class",
                "getClassLoader", "()Ljava/lang/ClassLoader;", false);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/ClassLoader",
                "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 3);

        // if (is == null) throw new UnsatisfiedLinkError
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        Label notNull = new Label();
        mv.visitJumpInsn(Opcodes.IFNONNULL, notNull);
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/UnsatisfiedLinkError");
        mv.visitInsn(Opcodes.DUP);
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("JNVM native library not found in JAR: ");
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder",
                "<init>", "(Ljava/lang/String;)V", false);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder",
                "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder",
                "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/UnsatisfiedLinkError",
                "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(Opcodes.ATHROW);

        mv.visitLabel(notNull);

        // File tmpDir = Files.createTempDirectory("jnvm-").toFile();
        mv.visitLdcInsn("jnvm-");
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/nio/file/attribute/FileAttribute");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/file/Files",
                "createTempDirectory",
                "(Ljava/lang/String;[Ljava/nio/file/attribute/FileAttribute;)Ljava/nio/file/Path;",
                false);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/nio/file/Path",
                "toFile", "()Ljava/io/File;", true);
        mv.visitVarInsn(Opcodes.ASTORE, 4);

        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/File",
                "deleteOnExit", "()V", false);

        // File tmpFile = new File(tmpDir, libName);
        mv.visitTypeInsn(Opcodes.NEW, "java/io/File");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/io/File",
                "<init>", "(Ljava/io/File;Ljava/lang/String;)V", false);
        mv.visitVarInsn(Opcodes.ASTORE, 5);

        mv.visitVarInsn(Opcodes.ALOAD, 5);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/File",
                "deleteOnExit", "()V", false);

        // FileOutputStream fos = new FileOutputStream(tmpFile);
        mv.visitTypeInsn(Opcodes.NEW, "java/io/FileOutputStream");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ALOAD, 5);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/io/FileOutputStream",
                "<init>", "(Ljava/io/File;)V", false);
        mv.visitVarInsn(Opcodes.ASTORE, 6);

        // byte[] buf = new byte[8192];
        mv.visitIntInsn(Opcodes.SIPUSH, 8192);
        mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
        mv.visitVarInsn(Opcodes.ASTORE, 7);

        // while loop
        Label loopStart = new Label();
        Label loopEnd = new Label();
        mv.visitLabel(loopStart);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitVarInsn(Opcodes.ALOAD, 7);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/InputStream",
                "read", "([B)I", false);
        mv.visitVarInsn(Opcodes.ISTORE, 8);
        mv.visitVarInsn(Opcodes.ILOAD, 8);
        mv.visitInsn(Opcodes.ICONST_M1);
        mv.visitJumpInsn(Opcodes.IF_ICMPEQ, loopEnd);

        mv.visitVarInsn(Opcodes.ALOAD, 6);
        mv.visitVarInsn(Opcodes.ALOAD, 7);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitVarInsn(Opcodes.ILOAD, 8);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/FileOutputStream",
                "write", "([BII)V", false);
        mv.visitJumpInsn(Opcodes.GOTO, loopStart);

        mv.visitLabel(loopEnd);

        // fos.close(); is.close();
        mv.visitVarInsn(Opcodes.ALOAD, 6);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/FileOutputStream",
                "close", "()V", false);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/InputStream",
                "close", "()V", false);

        // System.load(tmpFile.getAbsolutePath());
        mv.visitVarInsn(Opcodes.ALOAD, 5);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/File",
                "getAbsolutePath", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System",
                "load", "(Ljava/lang/String;)V", false);

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(5, 9);
        mv.visitEnd();
    }

    private void generateDetectTargetMethod(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                "detectTarget", "()Ljava/lang/String;", null, null);
        mv.visitCode();

        // arch
        mv.visitLdcInsn("os.arch");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System",
                "getProperty", "(Ljava/lang/String;)Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String",
                "toLowerCase", "()Ljava/lang/String;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 0);

        // os
        mv.visitLdcInsn("os.name");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System",
                "getProperty", "(Ljava/lang/String;)Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String",
                "toLowerCase", "()Ljava/lang/String;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 1);

        // zigArch = "x86_64"
        mv.visitLdcInsn("x86_64");
        mv.visitVarInsn(Opcodes.ASTORE, 2);

        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn("amd64");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String",
                "contains", "(Ljava/lang/CharSequence;)Z", false);
        Label notAmd64 = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, notAmd64);
        mv.visitLdcInsn("x86_64");
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        Label archDone = new Label();
        mv.visitJumpInsn(Opcodes.GOTO, archDone);

        mv.visitLabel(notAmd64);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn("aarch64");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String",
                "contains", "(Ljava/lang/CharSequence;)Z", false);
        Label notAarch64 = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, notAarch64);
        mv.visitLdcInsn("aarch64");
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        mv.visitJumpInsn(Opcodes.GOTO, archDone);

        mv.visitLabel(notAarch64);
        mv.visitLabel(archDone);

        // zigOs = "linux-gnu"
        mv.visitLdcInsn("linux-gnu");
        mv.visitVarInsn(Opcodes.ASTORE, 3);

        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitLdcInsn("windows");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String",
                "contains", "(Ljava/lang/CharSequence;)Z", false);
        Label notWindows = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, notWindows);
        mv.visitLdcInsn("windows-gnu");
        mv.visitVarInsn(Opcodes.ASTORE, 3);
        Label osDone = new Label();
        mv.visitJumpInsn(Opcodes.GOTO, osDone);

        mv.visitLabel(notWindows);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitLdcInsn("mac");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String",
                "contains", "(Ljava/lang/CharSequence;)Z", false);
        Label notMac = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, notMac);
        mv.visitLdcInsn("macos");
        mv.visitVarInsn(Opcodes.ASTORE, 3);
        mv.visitJumpInsn(Opcodes.GOTO, osDone);

        mv.visitLabel(notMac);
        mv.visitLabel(osDone);

        // return zigArch + "-" + zigOs
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder",
                "<init>", "()V", false);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder",
                "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitLdcInsn("-");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder",
                "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder",
                "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder",
                "toString", "()Ljava/lang/String;", false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(3, 4);
        mv.visitEnd();
    }

    private void generateDetectLibNameMethod(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                "detectLibName", "()Ljava/lang/String;", null, null);
        mv.visitCode();

        mv.visitLdcInsn("os.name");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System",
                "getProperty", "(Ljava/lang/String;)Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String",
                "toLowerCase", "()Ljava/lang/String;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 0);

        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn("windows");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String",
                "contains", "(Ljava/lang/CharSequence;)Z", false);
        Label notWin = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, notWin);
        mv.visitLdcInsn("customvm.dll");
        mv.visitInsn(Opcodes.ARETURN);

        mv.visitLabel(notWin);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn("mac");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String",
                "contains", "(Ljava/lang/CharSequence;)Z", false);
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
