package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * IALOAD instruction - load from int array (64-bit only, optimized)
 * Uses GetIntArrayRegion to avoid pin/unpin overhead
 */
public class IALoadInstruction extends Instruction {
    public IALoadInstruction() {
        super(0x2e, "IALOAD");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jint idx = frame.stack[--frame.sp].i;");
        w.println("                  jintArray arr = (jintArray)frame.stack[--frame.sp].l;");
        w.println("                  (*env)->GetIntArrayRegion(env, arr, idx, 1, &frame.stack[frame.sp++].i); }");
        pcIncBreak(w);
    }
}
