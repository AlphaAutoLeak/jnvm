package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * NEWARRAY instruction - create primitive type array
 */
public class NewArrayInstruction extends Instruction {
    public NewArrayInstruction() {
        super(0xbc, "NEWARRAY");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jint size = frame.stack[--frame.sp].i;");
        w.println("                  jobject arr = NULL;");
        w.println("                  switch (meta->intVal) {");
        w.println("                      case 4: arr = (*env)->NewBooleanArray(env, size); break;");
        w.println("                      case 5: arr = (*env)->NewCharArray(env, size); break;");
        w.println("                      case 6: arr = (*env)->NewFloatArray(env, size); break;");
        w.println("                      case 7: arr = (*env)->NewDoubleArray(env, size); break;");
        w.println("                      case 8: arr = (*env)->NewByteArray(env, size); break;");
        w.println("                      case 9: arr = (*env)->NewShortArray(env, size); break;");
        w.println("                      case 10: arr = (*env)->NewIntArray(env, size); break;");
        w.println("                      case 11: arr = (*env)->NewLongArray(env, size); break;");
        w.println("                  }");
        w.println("                  frame.stack[frame.sp++].l = arr; }");
        pcIncBreak(w);
    }
}