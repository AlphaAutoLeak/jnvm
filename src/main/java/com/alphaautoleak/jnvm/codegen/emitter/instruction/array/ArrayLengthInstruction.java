package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * ARRAYLENGTH instruction (64-bit only)
 */
public class ArrayLengthInstruction extends Instruction {
    public ArrayLengthInstruction() {
        super(0xbe, "ARRAYLENGTH");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                frame.stack[frame.sp-1].i = (*env)->GetArrayLength(env, frame.stack[frame.sp-1].l);");
        pcIncBreak(w);
    }
}
