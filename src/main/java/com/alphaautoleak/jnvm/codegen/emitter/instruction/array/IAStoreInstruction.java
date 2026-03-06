package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * IASTORE instruction - store to int array (optimized)
 */
public class IAStoreInstruction extends Instruction {
    public IAStoreInstruction() {
        super(0x4f, "IASTORE");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jint val = frame.stack[--frame.sp].i;");
        w.println("                  jint idx = frame.stack[--frame.sp].i;");
        w.println("                  jintArray arr = (jintArray)frame.stack[--frame.sp].l;");
        w.println("                  (*env)->SetIntArrayRegion(env, arr, idx, 1, &val); }");
        pcIncBreak(w);
    }
}