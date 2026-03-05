package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * BALOAD instruction - load from byte/boolean array
 */
public class BALoadInstruction extends Instruction {
    public BALoadInstruction() {
        super(0x33, "BALOAD");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jint idx = frame.stack[--frame.sp].i;");
        w.println("                  jbyteArray arr = (jbyteArray)frame.stack[--frame.sp].l;");
        w.println("                  jbyte* elems = (*env)->GetByteArrayElements(env, arr, NULL);");
        w.println("                  frame.stack[frame.sp++].i = elems[idx];");
        w.println("                  (*env)->ReleaseByteArrayElements(env, arr, elems, 0); }");
        pcIncBreak(w);
    }
}
