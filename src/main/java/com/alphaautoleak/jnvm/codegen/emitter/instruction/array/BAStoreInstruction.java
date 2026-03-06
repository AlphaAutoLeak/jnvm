package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * BASTORE instruction - store to byte/boolean array (optimized)
 */
public class BAStoreInstruction extends Instruction {
    public BAStoreInstruction() {
        super(0x54, "BASTORE");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jbyte val = (jbyte)frame.stack[--frame.sp].i;");
        w.println("                  jint idx = frame.stack[--frame.sp].i;");
        w.println("                  jbyteArray arr = (jbyteArray)frame.stack[--frame.sp].l;");
        w.println("                  (*env)->SetByteArrayRegion(env, arr, idx, 1, &val); }");
        pcIncBreak(w);
    }
}
