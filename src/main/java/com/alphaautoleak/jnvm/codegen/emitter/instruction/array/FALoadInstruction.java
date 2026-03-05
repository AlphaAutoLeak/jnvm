package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * FALOAD instruction - load from float array
 */
public class FALoadInstruction extends Instruction {
    public FALoadInstruction() {
        super(0x30, "FALOAD");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jint idx = frame.stack[--frame.sp].i;");
        w.println("                  jfloatArray arr = (jfloatArray)frame.stack[--frame.sp].l;");
        w.println("                  jfloat* elems = (*env)->GetFloatArrayElements(env, arr, NULL);");
        w.println("                  frame.stack[frame.sp].f = elems[idx];");
        w.println("                  frame.stackTypes[frame.sp++] = TYPE_FLOAT;");
        w.println("                  (*env)->ReleaseFloatArrayElements(env, arr, elems, 0); }");
        pcIncBreak(w);
    }
}