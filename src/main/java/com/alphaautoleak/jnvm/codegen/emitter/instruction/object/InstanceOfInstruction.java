package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * INSTANCEOF instruction
 */
public class InstanceOfInstruction extends Instruction {
    public InstanceOfInstruction() {
        super(0xc1, "INSTANCEOF");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jobject obj = frame.stack[frame.sp - 1].l;");
        w.println("                  if (!obj) {");
        w.println("                      frame.stack[frame.sp - 1].i = 0;"); // null instanceof X = false
        w.println("                      frame.stackTypes[frame.sp - 1] = TYPE_INT;");
        w.println("                  } else {");
        w.println("                      const char* clsName = vm_get_string(meta->classIdx);");
        w.println("                      jclass cls = (*env)->FindClass(env, clsName);");
        w.println("                      frame.stack[frame.sp - 1].i = (*env)->IsInstanceOf(env, obj, cls);");
        w.println("                      frame.stackTypes[frame.sp - 1] = TYPE_INT;");
        w.println("                  }");
        w.println("                }");
        pcIncBreak(w);
    }
}
