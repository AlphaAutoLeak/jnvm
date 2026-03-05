package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * CALOAD instruction - load from char array
 */
public class CALoadInstruction extends Instruction {
    public CALoadInstruction() {
        super(0x34, "CALOAD");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jint idx = frame.stack[--frame.sp].i;");
        w.println("                  jcharArray arr = (jcharArray)frame.stack[--frame.sp].l;");
        w.println("                  jchar* elems = (*env)->GetCharArrayElements(env, arr, NULL);");
        w.println("                  frame.stack[frame.sp++].i = elems[idx];");
        w.println("                  (*env)->ReleaseCharArrayElements(env, arr, elems, 0); }");
        pcIncBreak(w);
    }
}
