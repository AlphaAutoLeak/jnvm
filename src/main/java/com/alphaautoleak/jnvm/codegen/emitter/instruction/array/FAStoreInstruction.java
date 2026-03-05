package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * FASTORE instruction - store to float array
 */
public class FAStoreInstruction extends Instruction {
    public FAStoreInstruction() {
        super(0x51, "FASTORE");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jfloat val = frame.stack[--frame.sp].f;");
        w.println("                  jint idx = frame.stack[--frame.sp].i;");
        w.println("                  jfloatArray arr = (jfloatArray)frame.stack[--frame.sp].l;");
        w.println("                  jfloat* elems = (*env)->GetFloatArrayElements(env, arr, NULL);");
        w.println("                  elems[idx] = val;");
        w.println("                  (*env)->ReleaseFloatArrayElements(env, arr, elems, 0); }");
        pcIncBreak(w);
    }
}