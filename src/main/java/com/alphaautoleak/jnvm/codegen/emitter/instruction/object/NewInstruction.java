package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * NEW instruction - allocate object memory without calling constructor (64-bit only)
 */
public class NewInstruction extends Instruction {

    public NewInstruction() {
        super(0xbb, "NEW");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { const char* clsName = vm_get_string(meta->classIdx);");
        w.println("                  jclass cls = vm_find_class(env, clsName);");
        w.println("                  if (cls) {");
        w.println("                      frame.stack[frame.sp++].l = (*env)->AllocObject(env, cls);");
        w.println("                  } else {");
        w.println("                      VM_LOG(\"NEW: Class not found: %s\\n\", clsName);");
        w.println("                      frame.stack[frame.sp++].l = NULL;");
        w.println("                  } }");
        pcIncBreak(w);
    }
}