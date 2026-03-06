package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * SALOAD instruction - load from short array (64-bit only)
 */
public class SALoadInstruction extends Instruction {
    public SALoadInstruction() {
        super(0x35, "SALOAD");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jint idx = frame.stack[--frame.sp].i;");
        w.println("                  jshortArray arr = (jshortArray)frame.stack[--frame.sp].l;");
        w.println("                  jshort* elems = (*env)->GetShortArrayElements(env, arr, NULL);");
        w.println("                  frame.stack[frame.sp++].i = elems[idx];");
        w.println("                  (*env)->ReleaseShortArrayElements(env, arr, elems, 0); }");
        pcIncBreak(w);
    }
}
