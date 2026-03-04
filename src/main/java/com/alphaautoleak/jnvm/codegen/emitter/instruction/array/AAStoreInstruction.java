package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * AASTORE instruction - store to reference array
 */
public class AAStoreInstruction extends Instruction {
    public AAStoreInstruction() {
        super(0x53, "AASTORE");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jobject val = frame.stack[--frame.sp].l;");
        w.println("                  jint idx = frame.stack[--frame.sp].i;");
        w.println("                  jobjectArray arr = (jobjectArray)frame.stack[--frame.sp].l;");
        w.println("                  (*env)->SetObjectArrayElement(env, arr, idx, val); }");
        pcIncBreak(w);
    }
}