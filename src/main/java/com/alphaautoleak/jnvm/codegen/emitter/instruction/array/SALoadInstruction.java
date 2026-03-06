package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * SALOAD instruction - load from short array (64-bit only, optimized)
 */
public class SALoadInstruction extends Instruction {
    public SALoadInstruction() {
        super(0x35, "SALOAD");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jint idx = frame.stack[--frame.sp].i;");
        w.println("                  jshortArray arr = (jshortArray)frame.stack[--frame.sp].l;");
        w.println("                  jshort tmp;");
        w.println("                  (*env)->GetShortArrayRegion(env, arr, idx, 1, &tmp);");
        w.println("                  frame.stack[frame.sp++].i = tmp; }");
        pcIncBreak(w);
    }
}