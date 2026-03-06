package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * INSTANCEOF instruction (64-bit only)
 */
public class InstanceOfInstruction extends Instruction {
    public InstanceOfInstruction() {
        super(0xc1, "INSTANCEOF");
    }
    
    @Override
    public boolean needsMeta() {
        return true;  // 需要类信息
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jobject obj = frame.stack[frame.sp - 1].l;");
        w.println("                  if (!obj) {");
        w.println("                      frame.stack[frame.sp - 1].i = 0;");
        w.println("                  } else {");
        w.println("                      const char* clsName = vm_get_string(meta->classIdx);");
        w.println("                      jclass cls = vm_find_class(env, clsName);");
        w.println("                      frame.stack[frame.sp - 1].i = (*env)->IsInstanceOf(env, obj, cls);");
        w.println("                  }");
        w.println("                }");
        pcIncBreak(w);
    }
}