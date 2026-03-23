package com.alphaautoleak.jnvm.patcher;

import com.alphaautoleak.jnvm.asm.MethodInfo;
import com.alphaautoleak.jnvm.utils.MethodKeyUtil;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassPatcherTest {

    private static final String BRIDGE_CLASS = "bridge/Dispatcher";

    @Test
    void patchRewritesBootstrapEntryAsTrampolineAndAddsBridgeImpl() {
        String owner = "sample/BootstrapTarget";
        byte[] originalClass = classWithStaticMethod(owner, "bootstrap", "()I", false);
        MethodInfo protectedMethod = protectedMethod(owner, "bootstrap", "()I", 7);

        ClassNode patchedClass = patchClass(
                originalClass,
                Collections.singletonList(protectedMethod),
                Collections.singleton(MethodKeyUtil.of(owner, "bootstrap", "()I")),
                false
        );

        MethodNode bootstrapEntry = findMethod(patchedClass, "bootstrap", "()I");
        MethodNode bootstrapImpl = findGeneratedBootstrapImpl(patchedClass);

        assertNotNull(bootstrapEntry);
        assertNotNull(bootstrapImpl);
        assertTrue((bootstrapImpl.access & Opcodes.ACC_PRIVATE) != 0);
        assertTrue((bootstrapImpl.access & Opcodes.ACC_SYNTHETIC) != 0);
        assertTrue(hasMethodInvocation(bootstrapEntry, owner, bootstrapImpl.name, "()I"));
        assertTrue(hasMethodInvocation(bootstrapImpl, BRIDGE_CLASS, "executeInt", "(I[Ljava/lang/Object;)I"));
    }

    @Test
    void patchAddsSyntheticClinitForDirectNativeMethodsWhenMissing() {
        String owner = "sample/DirectNativeTarget";
        byte[] originalClass = classWithStaticMethod(owner, "run", "()V", false);
        MethodInfo protectedMethod = protectedMethod(owner, "run", "()V", 11);

        ClassNode patchedClass = patchClass(
                originalClass,
                Collections.singletonList(protectedMethod),
                Collections.<String>emptySet(),
                true
        );

        MethodNode runMethod = findMethod(patchedClass, "run", "()V");
        MethodNode clinitMethod = findMethod(patchedClass, "<clinit>", "()V");

        assertNotNull(runMethod);
        assertNotNull(clinitMethod);
        assertTrue((runMethod.access & Opcodes.ACC_NATIVE) != 0);
        assertEquals(0, runMethod.instructions.size());
        assertTrue(hasMethodInvocation(
                clinitMethod,
                BRIDGE_CLASS,
                MethodBodyRewriter.REGISTER_NATIVE_METHOD_NAME,
                MethodBodyRewriter.REGISTER_NATIVE_METHOD_DESC
        ));
    }

    @Test
    void patchPrependsNativeRegistrationToExistingClinitOnlyOnce() {
        String owner = "sample/ExistingClinitTarget";
        byte[] originalClass = classWithStaticMethod(owner, "run", "()V", true);
        MethodInfo protectedMethod = protectedMethod(owner, "run", "()V", 19);

        ClassNode patchedClass = patchClass(
                originalClass,
                Collections.singletonList(protectedMethod),
                Collections.<String>emptySet(),
                true
        );

        MethodNode clinitMethod = findMethod(patchedClass, "<clinit>", "()V");
        assertNotNull(clinitMethod);
        assertEquals(
                1,
                countMethodInvocations(
                        clinitMethod,
                        BRIDGE_CLASS,
                        MethodBodyRewriter.REGISTER_NATIVE_METHOD_NAME,
                        MethodBodyRewriter.REGISTER_NATIVE_METHOD_DESC
                )
        );

        MethodInsnNode firstInvocation = firstMethodInvocation(clinitMethod);
        assertNotNull(firstInvocation);
        assertEquals(BRIDGE_CLASS, firstInvocation.owner);
        assertEquals(MethodBodyRewriter.REGISTER_NATIVE_METHOD_NAME, firstInvocation.name);
    }

    private ClassNode patchClass(byte[] classBytes,
                                 List<MethodInfo> protectedMethods,
                                 Set<String> bootstrapMethodKeys,
                                 boolean directNativeRewrite) {
        MethodPatchRegistry patchRegistry = new MethodPatchRegistry(protectedMethods, directNativeRewrite);
        MethodBodyRewriter rewriter = new MethodBodyRewriter(
                BRIDGE_CLASS,
                patchRegistry.getMethodIdXorKey(),
                directNativeRewrite
        );
        ClassPatcher classPatcher = new ClassPatcher(
                BRIDGE_CLASS,
                directNativeRewrite,
                bootstrapMethodKeys,
                patchRegistry,
                rewriter
        );
        return readClass(classPatcher.patch(classBytes));
    }

    private MethodInfo protectedMethod(String owner, String name, String descriptor, int methodId) {
        MethodInfo methodInfo = new MethodInfo();
        methodInfo.setOwner(owner);
        methodInfo.setName(name);
        methodInfo.setDescriptor(descriptor);
        methodInfo.setMethodId(methodId);
        return methodInfo;
    }

    private byte[] classWithStaticMethod(String owner, String methodName, String descriptor, boolean includeClinit) {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        classWriter.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, owner, null, "java/lang/Object", null);

        writeConstructor(classWriter);
        writeStaticMethod(classWriter, methodName, descriptor);
        if (includeClinit) {
            writeClinit(classWriter);
        }

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

    private void writeStaticMethod(ClassWriter classWriter, String methodName, String descriptor) {
        MethodVisitor methodVisitor = classWriter.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                methodName,
                descriptor,
                null,
                null
        );
        methodVisitor.visitCode();
        if ("()I".equals(descriptor)) {
            methodVisitor.visitInsn(Opcodes.ICONST_1);
            methodVisitor.visitInsn(Opcodes.IRETURN);
        } else {
            methodVisitor.visitInsn(Opcodes.RETURN);
        }
        methodVisitor.visitMaxs(1, 0);
        methodVisitor.visitEnd();
    }

    private void writeClinit(ClassWriter classWriter) {
        MethodVisitor methodVisitor = classWriter.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        methodVisitor.visitCode();
        methodVisitor.visitInsn(Opcodes.RETURN);
        methodVisitor.visitMaxs(0, 0);
        methodVisitor.visitEnd();
    }

    private ClassNode readClass(byte[] classBytes) {
        ClassReader classReader = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        return classNode;
    }

    private MethodNode findMethod(ClassNode classNode, String name, String descriptor) {
        for (MethodNode methodNode : classNode.methods) {
            if (name.equals(methodNode.name) && descriptor.equals(methodNode.desc)) {
                return methodNode;
            }
        }
        return null;
    }

    private MethodNode findGeneratedBootstrapImpl(ClassNode classNode) {
        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.name.startsWith("__jnvm$bootstrap$impl$")) {
                return methodNode;
            }
        }
        return null;
    }

    private boolean hasMethodInvocation(MethodNode methodNode, String owner, String name, String descriptor) {
        return countMethodInvocations(methodNode, owner, name, descriptor) > 0;
    }

    private int countMethodInvocations(MethodNode methodNode, String owner, String name, String descriptor) {
        int count = 0;
        for (AbstractInsnNode instruction = methodNode.instructions.getFirst();
             instruction != null;
             instruction = instruction.getNext()) {
            if (!(instruction instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode methodInsn = (MethodInsnNode) instruction;
            if (owner.equals(methodInsn.owner)
                    && name.equals(methodInsn.name)
                    && descriptor.equals(methodInsn.desc)) {
                count++;
            }
        }
        return count;
    }

    private MethodInsnNode firstMethodInvocation(MethodNode methodNode) {
        for (AbstractInsnNode instruction = methodNode.instructions.getFirst();
             instruction != null;
             instruction = instruction.getNext()) {
            if (instruction instanceof MethodInsnNode) {
                return (MethodInsnNode) instruction;
            }
        }
        return null;
    }
}
