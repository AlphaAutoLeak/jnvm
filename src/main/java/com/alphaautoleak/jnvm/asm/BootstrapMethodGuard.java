package com.alphaautoleak.jnvm.asm;

import com.alphaautoleak.jnvm.utils.MethodKeyUtil;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;
import java.util.Set;

/**
 * Collects invokedynamic bootstrap entry methods found in class bytecode.
 */
class BootstrapMethodGuard {

    /** Methods used as invokedynamic bootstrap targets: owner.name.desc */
    private final Set<String> bootstrapMethodTargets = new HashSet<>();

    void scanClass(ClassNode cn) {
        for (MethodNode mn : cn.methods) {
            if (mn.instructions == null || mn.instructions.size() == 0) {
                continue;
            }
            for (AbstractInsnNode node = mn.instructions.getFirst(); node != null; node = node.getNext()) {
                if (!(node instanceof InvokeDynamicInsnNode)) {
                    continue;
                }
                InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) node;
                if (indy.bsm == null) {
                    continue;
                }
                String key = MethodKeyUtil.of(indy.bsm.getOwner(), indy.bsm.getName(), indy.bsm.getDesc());
                bootstrapMethodTargets.add(key);
            }
        }
    }

    Set<String> getBootstrapMethodTargetsSnapshot() {
        return new HashSet<>(bootstrapMethodTargets);
    }
}
