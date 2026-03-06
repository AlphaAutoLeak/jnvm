package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * SASTORE instruction - store to short array (optimized)
 */
public class SAStoreInstruction extends Instruction {
    public SAStoreInstruction() {
        super(0x56, "SASTORE");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jshort val = (jshort)frame.stack[--frame.sp].i;");
        w.println("                  jint idx = frame.stack[--frame.sp].i;");
        w.println("                  jshortArray arr = (jshortArray)frame.stack[--frame.sp].l;");
        w.println("                  (*env)->SetShortArrayRegion(env, arr, idx, 1, &val); }");
        pcIncBreak(w);
    }
}