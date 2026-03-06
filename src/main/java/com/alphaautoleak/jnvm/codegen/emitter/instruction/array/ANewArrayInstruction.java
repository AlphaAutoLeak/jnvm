package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * ANEWARRAY instruction - create reference type array (64-bit only)
 */
public class ANewArrayInstruction extends Instruction {
    public ANewArrayInstruction() {
        super(0xbd, "ANEWARRAY");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jint size = frame.stack[--frame.sp].i;");
        w.println("                  const char* elemCls = vm_get_string(meta->classIdx);");
        w.println("                  jclass cls = vm_find_class(env, elemCls);");
        w.println("                  frame.stack[frame.sp++].l = (*env)->NewObjectArray(env, size, cls, NULL); }");
        pcIncBreak(w);
    }
}