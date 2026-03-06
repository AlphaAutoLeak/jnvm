package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * BALOAD instruction - load from byte/boolean array (64-bit only, optimized)
 */
public class BALoadInstruction extends Instruction {
    public BALoadInstruction() {
        super(0x33, "BALOAD");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jint idx = frame.stack[--frame.sp].i;");
        w.println("                  jbyteArray arr = (jbyteArray)frame.stack[--frame.sp].l;");
        w.println("                  jbyte tmp;");
        w.println("                  (*env)->GetByteArrayRegion(env, arr, idx, 1, &tmp);");
        w.println("                  frame.stack[frame.sp++].i = tmp; }");
        pcIncBreak(w);
    }
}
