package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * GETFIELD instruction
 */
public class GetFieldInstruction extends Instruction {
    public GetFieldInstruction() {
        super(0xb4, "GETFIELD");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { if (!meta) { VM_LOG(\"GETFIELD: meta is NULL at pc=%d\\n\", frame.pc); frame.pc++; break; }");
        w.println("                  jobject obj = frame.stack[--frame.sp].l;");
        w.println("                  if (!obj) {");
        w.println("                      // Throw NullPointerException");
        w.println("                      jclass npeClass = (*env)->FindClass(env, \"java/lang/NullPointerException\");");
        w.println("                      if (npeClass) (*env)->ThrowNew(env, npeClass, \"\");");
        w.println("                      goto method_exit;");
        w.println("                  }");
        w.println("                  const char* owner = vm_get_string(meta->ownerIdx);");
        w.println("                  const char* name = vm_get_string(meta->nameIdx);");
        w.println("                  const char* desc = vm_get_string(meta->descIdx);");
        w.println("                  jclass cls = (*env)->FindClass(env, owner);");
        w.println("                  if (!cls) { VM_LOG(\"GETFIELD: Class not found: %s\\n\", owner); frame.pc++; break; }");
        w.println("                  jfieldID fid = (*env)->GetFieldID(env, cls, name, desc);");
        w.println("                  if (!fid) { VM_LOG(\"GETFIELD: Field not found: %s.%s\\n\", owner, name); (*env)->ExceptionClear(env); frame.pc++; break; }");
        w.println("                  char t = desc[0];");
        w.println("                  if (t == 'I' || t == 'B' || t == 'C' || t == 'S' || t == 'Z') {");
        w.println("                      frame.stack[frame.sp].i = (*env)->GetIntField(env, obj, fid);");
        w.println("                      frame.stackTypes[frame.sp++] = TYPE_INT;");
        w.println("                  } else if (t == 'J') {");
        w.println("                      frame.stack[frame.sp].j = (*env)->GetLongField(env, obj, fid);");
        w.println("                      frame.stackTypes[frame.sp++] = TYPE_LONG;");
        w.println("                  } else if (t == 'F') {");
        w.println("                      frame.stack[frame.sp].f = (*env)->GetFloatField(env, obj, fid);");
        w.println("                      frame.stackTypes[frame.sp++] = TYPE_FLOAT;");
        w.println("                  } else if (t == 'D') {");
        w.println("                      frame.stack[frame.sp].d = (*env)->GetDoubleField(env, obj, fid);");
        w.println("                      frame.stackTypes[frame.sp++] = TYPE_DOUBLE;");
        w.println("                  } else {");
        w.println("                      frame.stack[frame.sp].l = (*env)->GetObjectField(env, obj, fid);");
        w.println("                      frame.stackTypes[frame.sp++] = TYPE_REF;");
        w.println("                  } }");
        pcIncBreak(w);
    }
}