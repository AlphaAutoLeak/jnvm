package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * BASTORE instruction - store to byte/boolean array
 */
public class BAStoreInstruction extends Instruction {
    public BAStoreInstruction() {
        super(0x54, "BASTORE");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jbyte val = (jbyte)frame.stack[--frame.sp].i;");
        w.println("                  jint idx = frame.stack[--frame.sp].i;");
        w.println("                  jbyteArray arr = (jbyteArray)frame.stack[--frame.sp].l;");
        w.println("                  jbyte* elems = (*env)->GetByteArrayElements(env, arr, NULL);");
        w.println("                  elems[idx] = val;");
        w.println("                  (*env)->ReleaseByteArrayElements(env, arr, elems, 0); }");
        pcIncBreak(w);
    }
}