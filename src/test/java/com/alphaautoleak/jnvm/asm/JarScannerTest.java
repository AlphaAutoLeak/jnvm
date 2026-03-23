package com.alphaautoleak.jnvm.asm;

import com.alphaautoleak.jnvm.config.ProtectConfig;
import com.alphaautoleak.jnvm.crypto.OpcodeObfuscator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JarScannerTest {

    @TempDir
    Path tempDir;

    @Test
    void scanSkipsStackTraceSensitiveClassesAndTracksAffectedClasses() throws Exception {
        Path jarPath = tempDir.resolve("input.jar");
        writeJar(
                jarPath,
                classBytes("sample/ProtectedClass", false),
                classBytes("sample/SensitiveClass", true)
        );

        ProtectConfig config = new ProtectConfig();
        config.setInputJar(jarPath.toFile());
        config.getProtectRules().add("**");

        JarScanner scanner = new JarScanner(config, new OpcodeObfuscator());
        List<MethodInfo> methods = scanner.scan(jarPath.toFile());
        Set<String> affectedClasses = scanner.getAffectedClasses();

        assertEquals(1, methods.size());
        assertEquals("sample/ProtectedClass", methods.get(0).getOwner());
        assertEquals(1, affectedClasses.size());
        assertTrue(affectedClasses.contains("sample/ProtectedClass"));
    }

    private void writeJar(Path jarPath, byte[] protectedClass, byte[] sensitiveClass) throws IOException {
        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarPath))) {
            writeEntry(jarOutputStream, "sample/ProtectedClass.class", protectedClass);
            writeEntry(jarOutputStream, "sample/SensitiveClass.class", sensitiveClass);
        }
    }

    private void writeEntry(JarOutputStream jarOutputStream, String name, byte[] bytes) throws IOException {
        jarOutputStream.putNextEntry(new JarEntry(name));
        jarOutputStream.write(bytes);
        jarOutputStream.closeEntry();
    }

    private byte[] classBytes(String internalName, boolean stackTraceSensitive) {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        classWriter.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);

        writeConstructor(classWriter);
        writeWorkMethod(classWriter, stackTraceSensitive);

        classWriter.visitEnd();
        return classWriter.toByteArray();
    }

    private void writeConstructor(ClassWriter classWriter) {
        MethodVisitor methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        methodVisitor.visitCode();
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        methodVisitor.visitInsn(Opcodes.RETURN);
        methodVisitor.visitMaxs(1, 1);
        methodVisitor.visitEnd();
    }

    private void writeWorkMethod(ClassWriter classWriter, boolean stackTraceSensitive) {
        MethodVisitor methodVisitor = classWriter.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "work",
                "()V",
                null,
                null
        );
        methodVisitor.visitCode();
        if (stackTraceSensitive) {
            methodVisitor.visitTypeInsn(Opcodes.NEW, "java/lang/Throwable");
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Throwable", "<init>", "()V", false);
            methodVisitor.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Throwable",
                    "getStackTrace",
                    "()[Ljava/lang/StackTraceElement;",
                    false
            );
            methodVisitor.visitInsn(Opcodes.POP);
        }
        methodVisitor.visitInsn(Opcodes.RETURN);
        methodVisitor.visitMaxs(2, 0);
        methodVisitor.visitEnd();
    }
}
