package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * LALOAD instruction - load from long array (64-bit only, optimized)
 */
public class LALoadInstruction extends Instruction {
    public LALoadInstruction() {
        super(0x2f, "LALOAD");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jint idx = frame.stack[--frame.sp].i;");
        w.println("                  jlongArray arr = (jlongArray)frame.stack[--frame.sp].l;");
        w.println("                  if (arr == NULL) {");
        w.println("                      jclass npeClass = vm_find_class(env, \"java/lang/NullPointerException\");");
        w.println("                      if (npeClass) (*env)->ThrowNew(env, npeClass, \"null array\");");
        w.println("                      _hasException = 1; goto method_exit;");
        w.println("                  }");
        w.println("                  (*env)->GetLongArrayRegion(env, arr, idx, 1, &frame.stack[frame.sp++].j); }");
        pcIncBreak(w);
    }
}
