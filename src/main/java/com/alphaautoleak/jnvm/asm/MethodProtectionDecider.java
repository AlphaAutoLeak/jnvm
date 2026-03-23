package com.alphaautoleak.jnvm.asm;

import com.alphaautoleak.jnvm.config.ProtectConfig;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

final class MethodProtectionDecider {

    private final ProtectConfig config;
    private final List<String> annotationDescs;

    MethodProtectionDecider(ProtectConfig config) {
        this.config = config;
        this.annotationDescs = config.getAnnotationRules();
    }

    boolean isClassAnnotated(ClassNode classNode) {
        if (annotationDescs.isEmpty() || classNode.visibleAnnotations == null) {
            return false;
        }
        for (AnnotationNode annotation : classNode.visibleAnnotations) {
            if (annotationDescs.contains(annotation.desc)) {
                return true;
            }
        }
        return false;
    }

    boolean isMethodEligible(MethodNode methodNode) {
        if ((methodNode.access & Opcodes.ACC_ABSTRACT) != 0 || (methodNode.access & Opcodes.ACC_NATIVE) != 0) {
            return false;
        }
        if (methodNode.instructions == null || methodNode.instructions.size() == 0) {
            return false;
        }
        return !"<init>".equals(methodNode.name);
    }

    boolean shouldProtectMethod(String className, boolean classAnnotated, MethodNode methodNode) {
        if (config.shouldProtect(className, methodNode.name)) {
            return true;
        }
        if (classAnnotated) {
            return true;
        }
        if (annotationDescs.isEmpty() || methodNode.visibleAnnotations == null) {
            return false;
        }
        for (AnnotationNode annotation : methodNode.visibleAnnotations) {
            if (annotationDescs.contains(annotation.desc)) {
                return true;
            }
        }
        return false;
    }
}
