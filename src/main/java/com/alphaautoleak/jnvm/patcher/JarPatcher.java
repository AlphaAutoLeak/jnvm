package com.alphaautoleak.jnvm.patcher;

import com.alphaautoleak.jnvm.asm.MethodInfo;
import com.alphaautoleak.jnvm.cli.CliReporter;
import com.alphaautoleak.jnvm.utils.BridgePackageNameGenerator;
import com.alphaautoleak.jnvm.utils.MethodKeyUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Rewrites protected method bodies to call VMBridge.execute()
 */
public class JarPatcher {

    private final Set<String> affectedClasses;
    private final String bridgeClass;
    private final int methodIdXorKey;
    private final boolean directNativeRewrite;
    private final Set<String> bootstrapMethodKeys;
    private final MethodPatchRegistry patchRegistry;

    private final MethodBodyRewriter rewriter;
    private final BridgeClassGenerator bridgeGenerator;

    public JarPatcher(List<MethodInfo> protectedMethods,
                      Set<String> affectedClasses,
                      Set<String> bootstrapMethodKeys,
                      boolean directNativeRewrite) {
        this.affectedClasses = affectedClasses;
        this.patchRegistry = new MethodPatchRegistry(protectedMethods, directNativeRewrite);
        this.bootstrapMethodKeys = MethodKeyUtil.normalizeAll(bootstrapMethodKeys);
        this.bridgeClass = BridgePackageNameGenerator.generate();
        this.directNativeRewrite = directNativeRewrite;
        this.methodIdXorKey = patchRegistry.getMethodIdXorKey();

        this.rewriter = new MethodBodyRewriter(bridgeClass, methodIdXorKey, directNativeRewrite);
        this.bridgeGenerator = new BridgeClassGenerator(bridgeClass);
    }

    public String getBridgeClass() {
        return bridgeClass;
    }

    public int getMethodIdXorKey() {
        return methodIdXorKey;
    }

    public void patch(File inputJar, File outputJar) throws IOException {
        CliReporter.tagged("PATCH", "Input:  " + inputJar);
        CliReporter.tagged("PATCH", "Output: " + outputJar);
        CliReporter.tagged("PATCH", "Bridge class: " + bridgeClass.replace('/', '.'));

        ClassPatcher classPatcher = new ClassPatcher(
                bridgeClass,
                directNativeRewrite,
                bootstrapMethodKeys,
                patchRegistry,
                rewriter
        );
        JarPatchSession patchSession = new JarPatchSession(
                inputJar,
                outputJar,
                affectedClasses,
                bridgeClass,
                bridgeGenerator,
                classPatcher
        );
        int patchedCount = patchSession.run();

        CliReporter.tagged("PATCH", "Patched " + patchedCount + " classes.");
    }
}
