package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * LASTORE instruction - store to long array
 */
public class LAStoreInstruction extends Instruction {
    public LAStoreInstruction() {
        super(0x50, "LASTORE");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jlong val = frame.stack[--frame.sp].j;");
        w.println("                  jint idx = frame.stack[--frame.sp].i;");
        w.println("                  jobject arrObj = frame.stack[--frame.sp].l;");
        w.println("                  VM_LOG(\"LASTORE: val=%lld, idx=%d, arr=%p\\n\", (long long)val, idx, arrObj);");
        w.println("                  fflush(stdout);");
        w.println("                  if (arrObj == NULL) {");
        w.println("                      VM_LOG(\"LASTORE: null array\\n\");");
        w.println("                      jclass npeClass = (*env)->FindClass(env, \"java/lang/NullPointerException\");");
        w.println("                      if (npeClass) (*env)->ThrowNew(env, npeClass, \"null array\");");
        w.println("                      goto method_exit;");
        w.println("                  }");
        w.println("                  if (!(*env)->IsInstanceOf(env, arrObj, (*env)->FindClass(env, \"[J\"))) {");
        w.println("                      VM_LOG(\"LASTORE: not a long array\\n\");");
        w.println("                      jclass exc = (*env)->FindClass(env, \"java/lang/ArrayStoreException\");");
        w.println("                      if (exc) (*env)->ThrowNew(env, exc, \"not a long array\");");
        w.println("                      goto method_exit;");
        w.println("                  }");
        w.println("                  jlongArray arr = (jlongArray)arrObj;");
        w.println("                  jsize arrLen = (*env)->GetArrayLength(env, arr);");
        w.println("                  VM_LOG(\"LASTORE: arrLen=%d, idx=%d\\n\", arrLen, idx);");
        w.println("                  fflush(stdout);");
        w.println("                  if (idx < 0 || idx >= arrLen) {");
        w.println("                      VM_LOG(\"LASTORE: ArrayIndexOutOfBoundsException\\n\");");
        w.println("                      jclass exc = (*env)->FindClass(env, \"java/lang/ArrayIndexOutOfBoundsException\");");
        w.println("                      if (exc) (*env)->ThrowNew(env, exc, \"Array index out of bounds\");");
        w.println("                      goto method_exit;");
        w.println("                  }");
        w.println("                  jlong* elems = (*env)->GetLongArrayElements(env, arr, NULL);");
        w.println("                  elems[idx] = val;");
        w.println("                  (*env)->ReleaseLongArrayElements(env, arr, elems, 0); }");
        pcIncBreak(w);
    }
}
