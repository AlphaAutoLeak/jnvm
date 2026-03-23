package com.alphaautoleak.jnvm.patcher;

import com.alphaautoleak.jnvm.utils.MethodKeyUtil;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class ProtectedClassRewriter {

    private final boolean directNativeRewrite;
    private final Set<String> bootstrapMethodKeys;
    private final MethodPatchRegistry patchRegistry;
    private final MethodBodyRewriter rewriter;
    private final ClinitRegistrationCoordinator clinitCoordinator;

    ProtectedClassRewriter(boolean directNativeRewrite,
                           Set<String> bootstrapMethodKeys,
                           MethodPatchRegistry patchRegistry,
                           MethodBodyRewriter rewriter,
                           ClinitRegistrationCoordinator clinitCoordinator) {
        this.directNativeRewrite = directNativeRewrite;
        this.bootstrapMethodKeys = bootstrapMethodKeys;
        this.patchRegistry = patchRegistry;
        this.rewriter = rewriter;
        this.clinitCoordinator = clinitCoordinator;
    }

    void rewrite(ClassNode classNode) {
        boolean classHasDirectNativeMethods = directNativeRewrite && patchRegistry.hasDirectNativeMethods(classNode.name);
        List<MethodNode> appendedMethods = rewriteProtectedMethods(classNode, classHasDirectNativeMethods);
        if (!appendedMethods.isEmpty()) {
            classNode.methods.addAll(appendedMethods);
        }

        if (shouldEnsureNativeRegistration(classNode.name, classHasDirectNativeMethods)) {
            clinitCoordinator.ensureClassNativeRegistration(classNode);
        }
    }

    private List<MethodNode> rewriteProtectedMethods(ClassNode classNode, boolean classHasDirectNativeMethods) {
        List<MethodNode> appendedMethods = new ArrayList<>();
        for (MethodNode method : classNode.methods) {
            Integer methodId = patchRegistry.findMethodId(classNode.name, method.name, method.desc);
            if (methodId == null) {
                continue;
            }

            if (isBootstrapEntryMethod(classNode.name, method)) {
                appendedMethods.add(rewriter.rewriteBootstrapEntryAsTrampoline(classNode, method, methodId));
                continue;
            }

            rewriter.rewrite(classNode, method, methodId, classHasDirectNativeMethods);
        }
        return appendedMethods;
    }

    private boolean shouldEnsureNativeRegistration(String className, boolean classHasDirectNativeMethods) {
        return directNativeRewrite && classHasDirectNativeMethods && !patchRegistry.hasProtectedClinit(className);
    }

    private boolean isBootstrapEntryMethod(String owner, MethodNode method) {
        if ("<clinit>".equals(method.name) || "<init>".equals(method.name)) {
            return false;
        }
        return bootstrapMethodKeys.contains(MethodKeyUtil.of(owner, method.name, method.desc));
    }
}
