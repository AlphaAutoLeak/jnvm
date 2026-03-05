package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * NEW instruction - allocate object memory without calling constructor
 */
public class NewInstruction extends Instruction {

    public NewInstruction() {
        super(0xbb, "NEW");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { const char* clsName = vm_strings[meta->classIdx].data;");
        w.println("                  jclass cls = (*env)->FindClass(env, clsName);");
        w.println("                  if (cls) {");
        w.println("                      frame.stack[frame.sp].l = (*env)->AllocObject(env, cls);");
        w.println("                      frame.stackTypes[frame.sp++] = TYPE_REF;");
        w.println("                  } else {");
        w.println("                      VM_LOG(\"NEW: Class not found: %s\\n\", clsName);");
        w.println("                      frame.stack[frame.sp].l = NULL;");
        w.println("                      frame.stackTypes[frame.sp++] = TYPE_REF;");
        w.println("                  } }");
        pcIncBreak(w);
    }
}
