package com.alphaautoleak.jnvm.asm;

import com.alphaautoleak.jnvm.config.ProtectConfig;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodProtectionDeciderTest {

    @Test
    void isMethodEligibleRejectsUnsupportedMethods() {
        MethodProtectionDecider decider = new MethodProtectionDecider(new ProtectConfig());

        MethodNode constructor = methodNode(Opcodes.ACC_PUBLIC, "<init>", true);
        MethodNode nativeMethod = methodNode(Opcodes.ACC_NATIVE, "nativeRun", false);
        MethodNode abstractMethod = methodNode(Opcodes.ACC_ABSTRACT, "abstractRun", false);
        MethodNode emptyMethod = methodNode(Opcodes.ACC_PUBLIC, "emptyRun", false);

        assertFalse(decider.isMethodEligible(constructor));
        assertFalse(decider.isMethodEligible(nativeMethod));
        assertFalse(decider.isMethodEligible(abstractMethod));
        assertFalse(decider.isMethodEligible(emptyMethod));
    }

    @Test
    void shouldProtectMethodSupportsExplicitRuleAndAnnotations() {
        ProtectConfig config = new ProtectConfig();
        config.getProtectRules().add("sample.Target#byRule");
        config.getProtectRules().add("@sample.ProtectMe");

        MethodProtectionDecider decider = new MethodProtectionDecider(config);
        ClassNode plainClass = new ClassNode();
        plainClass.name = "sample/Target";

        ClassNode annotatedClass = new ClassNode();
        annotatedClass.name = "sample/Annotated";
        annotatedClass.visibleAnnotations = new ArrayList<AnnotationNode>();
        annotatedClass.visibleAnnotations.add(new AnnotationNode("Lsample/ProtectMe;"));

        MethodNode ruledMethod = methodNode(Opcodes.ACC_PUBLIC, "byRule", true);
        MethodNode classAnnotatedMethod = methodNode(Opcodes.ACC_PUBLIC, "fromClassAnnotation", true);
        MethodNode methodAnnotatedMethod = methodNode(Opcodes.ACC_PUBLIC, "fromMethodAnnotation", true);
        methodAnnotatedMethod.visibleAnnotations = new ArrayList<AnnotationNode>();
        methodAnnotatedMethod.visibleAnnotations.add(new AnnotationNode("Lsample/ProtectMe;"));

        assertTrue(decider.shouldProtectMethod("sample/Target", false, ruledMethod));
        assertTrue(decider.isClassAnnotated(annotatedClass));
        assertTrue(decider.shouldProtectMethod("sample/Annotated", true, classAnnotatedMethod));
        assertTrue(decider.shouldProtectMethod("sample/Target", false, methodAnnotatedMethod));
        assertFalse(decider.isClassAnnotated(plainClass));
    }

    private MethodNode methodNode(int access, String name, boolean withInstructions) {
        MethodNode methodNode = new MethodNode(access, name, "()V", null, null);
        if (withInstructions) {
            methodNode.instructions.add(new InsnNode(Opcodes.RETURN));
        }
        return methodNode;
    }
}
