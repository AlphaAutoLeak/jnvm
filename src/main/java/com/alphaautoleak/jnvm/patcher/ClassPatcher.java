package com.alphaautoleak.jnvm.patcher;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.util.Set;

final class ClassPatcher {

    private final ProtectedClassRewriter protectedClassRewriter;

    ClassPatcher(String bridgeClass,
                 boolean directNativeRewrite,
                 Set<String> bootstrapMethodKeys,
                 MethodPatchRegistry patchRegistry,
                 MethodBodyRewriter rewriter) {
        this.protectedClassRewriter = new ProtectedClassRewriter(
                directNativeRewrite,
                bootstrapMethodKeys,
                patchRegistry,
                rewriter,
                new ClinitRegistrationCoordinator(bridgeClass, rewriter)
        );
    }

    byte[] patch(byte[] classBytes) {
        ClassReader cr = new ClassReader(classBytes);
        ClassNode cn = new ClassNode(Opcodes.ASM9);
        cr.accept(cn, 0);

        protectedClassRewriter.rewrite(cn);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }
}
