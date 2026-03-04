package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * NEW 指令 - 只分配对象内存，不调用构造函�? */
public class NewInstruction extends Instruction {
    public NewInstruction() {
        super(0xbb, "NEW");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { const char* clsName = vm_strings[meta->classIdx].data;");
        w.println("                  jclass cls = (*env)->FindClass(env, clsName);");
        w.println("                  if (cls) frame.stack[frame.sp++].l = (*env)->AllocObject(env, cls);");
        w.println("                  else { VM_LOG(\"NEW: Class not found: %s\\n\", clsName); frame.stack[frame.sp++].l = NULL; } }");
        pcIncBreak(w);
    }
}