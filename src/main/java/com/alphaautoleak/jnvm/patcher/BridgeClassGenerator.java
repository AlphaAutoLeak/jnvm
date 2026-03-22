package com.alphaautoleak.jnvm.patcher;

import org.objectweb.asm.ClassWriter;

/**
 * Generates VMBridge class bytecode
 */
class BridgeClassGenerator {

    private final String bridgeClass;

    BridgeClassGenerator(String bridgeClass) {
        this.bridgeClass = bridgeClass;
    }

    byte[] generate() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        BridgeClassStructureWriter.writeClassHeader(cw, bridgeClass);
        BridgeNativeMethodWriter.write(cw);
        new BridgeRuntimeBootstrapWriter(bridgeClass).write(cw);
        BridgePlatformDetectionWriter.write(cw);
        BridgeConstructorWriter.writePrivateConstructor(cw);

        cw.visitEnd();
        return cw.toByteArray();
    }
}
