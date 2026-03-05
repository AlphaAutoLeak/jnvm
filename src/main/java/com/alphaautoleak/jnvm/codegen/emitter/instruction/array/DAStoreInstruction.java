package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * DASTORE instruction - store to double array
 */
public class DAStoreInstruction extends Instruction {
    public DAStoreInstruction() {
        super(0x52, "DASTORE");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jdouble val = frame.stack[--frame.sp].d;");
        w.println("                  jint idx = frame.stack[--frame.sp].i;");
        w.println("                  jdoubleArray arr = (jdoubleArray)frame.stack[--frame.sp].l;");
        w.println("                  jdouble* elems = (*env)->GetDoubleArrayElements(env, arr, NULL);");
        w.println("                  elems[idx] = val;");
        w.println("                  (*env)->ReleaseDoubleArrayElements(env, arr, elems, 0); }");
        pcIncBreak(w);
    }
}