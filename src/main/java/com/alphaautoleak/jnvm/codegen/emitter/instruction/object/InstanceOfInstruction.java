package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * INSTANCEOF 指令
 */
public class InstanceOfInstruction extends Instruction {
    public InstanceOfInstruction() {
        super(0xc1, "INSTANCEOF");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jobject obj = frame.stack[frame.sp - 1].l;");
        w.println("                  const char* clsName = vm_strings[meta->classIdx].data;");
        w.println("                  jclass cls = (*env)->FindClass(env, clsName);");
        w.println("                  frame.stack[frame.sp - 1].i = (*env)->IsInstanceOf(env, obj, cls); }");
        pcIncBreak(w);
    }
}
