package com.alphaautoleak.jnvm.patcher;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

final class BridgeNativeMethodWriter {

    private BridgeNativeMethodWriter() {
    }

    static void write(ClassWriter cw) {
        writeNative(cw, "__jnvm$registerClassNatives", "(Ljava/lang/Class;)V");
        writeNative(cw, "executeVoid", "(I[Ljava/lang/Object;)V");
        writeNative(cw, "executeInt", "(I[Ljava/lang/Object;)I");
        writeNative(cw, "executeLong", "(I[Ljava/lang/Object;)J");
        writeNative(cw, "executeFloat", "(I[Ljava/lang/Object;)F");
        writeNative(cw, "executeDouble", "(I[Ljava/lang/Object;)D");
        writeNative(cw, "executeObject", "(I[Ljava/lang/Object;)Ljava/lang/Object;");
    }

    private static void writeNative(ClassWriter cw, String name, String desc) {
        cw.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_NATIVE,
                name,
                desc,
                null,
                null
        ).visitEnd();
    }
}
