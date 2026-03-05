package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * SASTORE instruction - store to short array
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
        w.println("                  jshort* elems = (*env)->GetShortArrayElements(env, arr, NULL);");
        w.println("                  elems[idx] = val;");
        w.println("                  (*env)->ReleaseShortArrayElements(env, arr, elems, 0); }");
        pcIncBreak(w);
    }
}
