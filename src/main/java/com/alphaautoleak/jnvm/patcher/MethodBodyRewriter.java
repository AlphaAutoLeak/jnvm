package com.alphaautoleak.jnvm.patcher;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Rewrites protected method bodies to native bridge calls.
 * Supports legacy bridge mode and direct-native-rewrite mode.
 */
public class MethodBodyRewriter {

    static final String REGISTER_NATIVE_METHOD_NAME = "__jnvm$registerClassNatives";
    static final String REGISTER_NATIVE_METHOD_DESC = "(Ljava/lang/Class;)V";

    private final boolean directNativeRewrite;
    private final ClinitNativeRegistrationHelper clinitHelper;
    private final LegacyBridgeMethodRewriter legacyRewriter;
    private final DirectNativeMethodRewriter directNativeRewriter;
    private final BootstrapTrampolineRewriter bootstrapRewriter;

    MethodBodyRewriter(String bridgeClass, int methodIdXorKey, boolean directNativeRewrite) {
        this.directNativeRewrite = directNativeRewrite;
        this.clinitHelper = new ClinitNativeRegistrationHelper(bridgeClass);
        this.legacyRewriter = new LegacyBridgeMethodRewriter(bridgeClass, methodIdXorKey);
        this.directNativeRewriter = new DirectNativeMethodRewriter(bridgeClass, methodIdXorKey, clinitHelper);
        this.bootstrapRewriter = new BootstrapTrampolineRewriter(legacyRewriter);
    }

    void rewrite(ClassNode cn, MethodNode mn, int methodId, boolean classHasDirectNativeMethods) {
        if (directNativeRewrite) {
            if ("<clinit>".equals(mn.name)) {
                directNativeRewriter.rewriteClinit(cn, mn, methodId, classHasDirectNativeMethods);
            } else {
                directNativeRewriter.rewrite(mn);
            }
            return;
        }
        legacyRewriter.rewrite(cn, mn, methodId);
    }

    MethodNode rewriteBootstrapEntryAsTrampoline(ClassNode cn, MethodNode bootstrapEntry, int methodId) {
        return bootstrapRewriter.rewrite(cn, bootstrapEntry, methodId);
    }

    void prependRegisterCallToClinit(ClassNode cn, MethodNode clinit) {
        clinitHelper.prependRegisterCallToClinit(cn, clinit);
    }

    MethodNode createSyntheticRegisterOnlyClinit(String ownerClassInternalName) {
        return clinitHelper.createSyntheticRegisterOnlyClinit(ownerClassInternalName);
    }
}
