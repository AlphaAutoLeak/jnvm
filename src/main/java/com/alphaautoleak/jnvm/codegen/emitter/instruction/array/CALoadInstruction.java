package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * CALOAD instruction - load from char array (64-bit only, optimized)
 */
public class CALoadInstruction extends Instruction {
    public CALoadInstruction() {
        super(0x34, "CALOAD");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jint idx = frame.stack[--frame.sp].i;");
        w.println("                  jcharArray arr = (jcharArray)frame.stack[--frame.sp].l;");
        w.println("                  jchar tmp;");
        w.println("                  (*env)->GetCharArrayRegion(env, arr, idx, 1, &tmp);");
        w.println("                  frame.stack[frame.sp++].i = tmp; }");
        pcIncBreak(w);
    }
}
