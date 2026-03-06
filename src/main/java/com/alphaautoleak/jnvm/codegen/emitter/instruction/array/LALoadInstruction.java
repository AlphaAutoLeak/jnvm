package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * LALOAD instruction - load from long array (64-bit only)
 */
public class LALoadInstruction extends Instruction {
    public LALoadInstruction() {
        super(0x2f, "LALOAD");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jint idx = frame.stack[--frame.sp].i;");
        w.println("                  jobject arrObj = frame.stack[--frame.sp].l;");
        w.println("                  if (arrObj == NULL) {");
        w.println("                      VM_LOG(\"LALOAD: null array\\n\");");
        w.println("                      jclass npeClass = (*env)->FindClass(env, \"java/lang/NullPointerException\");");
        w.println("                      if (npeClass) (*env)->ThrowNew(env, npeClass, \"null array\");");
        w.println("                      goto method_exit;");
        w.println("                  }");
        w.println("                  jlongArray arr = (jlongArray)arrObj;");
        w.println("                  jlong* elems = (*env)->GetLongArrayElements(env, arr, NULL);");
        w.println("                  frame.stack[frame.sp++].j = elems[idx];");
        w.println("                  (*env)->ReleaseLongArrayElements(env, arr, elems, 0); }");
        pcIncBreak(w);
    }
}