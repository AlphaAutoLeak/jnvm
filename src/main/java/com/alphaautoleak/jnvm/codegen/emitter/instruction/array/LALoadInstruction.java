package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * LALOAD instruction - load from long array
 */
public class LALoadInstruction extends Instruction {
    public LALoadInstruction() {
        super(0x2f, "LALOAD");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jint idx = frame.stack[--frame.sp].i;");
        w.println("                  jlongArray arr = (jlongArray)frame.stack[--frame.sp].l;");
        w.println("                  jlong* elems = (*env)->GetLongArrayElements(env, arr, NULL);");
        w.println("                  frame.stack[frame.sp++].j = elems[idx];");
        w.println("                  (*env)->ReleaseLongArrayElements(env, arr, elems, 0); }");
        pcIncBreak(w);
    }
}
