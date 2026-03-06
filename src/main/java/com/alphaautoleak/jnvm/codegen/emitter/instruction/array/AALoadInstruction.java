package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * AALOAD instruction - load from reference array (64-bit only)
 */
public class AALoadInstruction extends Instruction {
    public AALoadInstruction() {
        super(0x32, "AALOAD");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jint idx = frame.stack[--frame.sp].i;");
        w.println("                  jobjectArray arr = (jobjectArray)frame.stack[--frame.sp].l;");
        w.println("                  frame.stack[frame.sp++].l = (*env)->GetObjectArrayElement(env, arr, idx); }");
        pcIncBreak(w);
    }
}