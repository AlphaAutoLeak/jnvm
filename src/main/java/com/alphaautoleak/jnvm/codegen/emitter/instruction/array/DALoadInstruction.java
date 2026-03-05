package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * DALOAD instruction - load from double array
 */
public class DALoadInstruction extends Instruction {
    public DALoadInstruction() {
        super(0x31, "DALOAD");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jint idx = frame.stack[--frame.sp].i;");
        w.println("                  jdoubleArray arr = (jdoubleArray)frame.stack[--frame.sp].l;");
        w.println("                  jdouble* elems = (*env)->GetDoubleArrayElements(env, arr, NULL);");
        w.println("                  frame.stack[frame.sp++].d = elems[idx];");
        w.println("                  (*env)->ReleaseDoubleArrayElements(env, arr, elems, 0); }");
        pcIncBreak(w);
    }
}