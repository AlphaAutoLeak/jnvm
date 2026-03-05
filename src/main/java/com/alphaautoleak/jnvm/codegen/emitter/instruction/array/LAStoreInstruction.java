package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * LASTORE instruction - store to long array
 */
public class LAStoreInstruction extends Instruction {
    public LAStoreInstruction() {
        super(0x50, "LASTORE");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jlong val = frame.stack[--frame.sp].j;");
        w.println("                  jint idx = frame.stack[--frame.sp].i;");
        w.println("                  jlongArray arr = (jlongArray)frame.stack[--frame.sp].l;");
        w.println("                  jlong* elems = (*env)->GetLongArrayElements(env, arr, NULL);");
        w.println("                  elems[idx] = val;");
        w.println("                  (*env)->ReleaseLongArrayElements(env, arr, elems, 0); }");
        pcIncBreak(w);
    }
}
