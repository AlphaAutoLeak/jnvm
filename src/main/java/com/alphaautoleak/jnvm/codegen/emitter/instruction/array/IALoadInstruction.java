package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * IALOAD instruction - load from int array
 */
public class IALoadInstruction extends Instruction {
    public IALoadInstruction() {
        super(0x2e, "IALOAD");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jint idx = frame.stack[--frame.sp].i;");
        w.println("                  jintArray arr = (jintArray)frame.stack[--frame.sp].l;");
        w.println("                  jint* elems = (*env)->GetIntArrayElements(env, arr, NULL);");
        w.println("                  frame.stack[frame.sp++].i = elems[idx];");
        w.println("                  (*env)->ReleaseIntArrayElements(env, arr, elems, 0); }");
        pcIncBreak(w);
    }
}
