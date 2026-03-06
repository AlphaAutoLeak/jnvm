package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * FALOAD instruction - load from float array (64-bit only, optimized)
 */
public class FALoadInstruction extends Instruction {
    public FALoadInstruction() {
        super(0x30, "FALOAD");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jint idx = frame.stack[--frame.sp].i;");
        w.println("                  jfloatArray arr = (jfloatArray)frame.stack[--frame.sp].l;");
        w.println("                  (*env)->GetFloatArrayRegion(env, arr, idx, 1, &frame.stack[frame.sp++].f); }");
        pcIncBreak(w);
    }
}