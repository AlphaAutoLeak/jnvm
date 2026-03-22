package com.alphaautoleak.jnvm.patcher;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

final class BridgeClassStructureWriter {

    private BridgeClassStructureWriter() {
    }

    static void writeClassHeader(ClassWriter cw, String bridgeClass) {
        cw.visit(
                Opcodes.V1_8,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                bridgeClass,
                null,
                "java/lang/Object",
                null
        );
    }
}
