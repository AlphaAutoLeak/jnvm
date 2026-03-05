package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * CASTORE instruction - store to char array
 */
public class CAStoreInstruction extends Instruction {
    public CAStoreInstruction() {
        super(0x55, "CASTORE");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jchar val = (jchar)frame.stack[--frame.sp].i;");
        w.println("                  jint idx = frame.stack[--frame.sp].i;");
        w.println("                  jcharArray arr = (jcharArray)frame.stack[--frame.sp].l;");
        w.println("                  jchar* elems = (*env)->GetCharArrayElements(env, arr, NULL);");
        w.println("                  elems[idx] = val;");
        w.println("                  (*env)->ReleaseCharArrayElements(env, arr, elems, 0); }");
        pcIncBreak(w);
    }
}
