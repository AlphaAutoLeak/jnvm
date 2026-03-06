package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * GETFIELD instruction (64-bit only)
 */
public class GetFieldInstruction extends Instruction {
    public GetFieldInstruction() {
        super(0xb4, "GETFIELD");
    }
    
    @Override
    public boolean needsMeta() {
        return true;  // 需要字段信息
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { if (!meta) { VM_LOG(\"GETFIELD: meta is NULL at pc=%d\\n\", frame.pc); frame.pc++; break; }");
        w.println("                  jobject obj = frame.stack[--frame.sp].l;");
        w.println("                  if (!obj) {");
        w.println("                      jclass npeClass = vm_find_class(env, \"java/lang/NullPointerException\");");
        w.println("                      if (npeClass) (*env)->ThrowNew(env, npeClass, \"\");");
        w.println("                      goto method_exit;");
        w.println("                  }");
        w.println("                  const char* owner = vm_get_string(meta->ownerIdx);");
        w.println("                  const char* name = vm_get_string(meta->nameIdx);");
        w.println("                  const char* desc = vm_get_string(meta->descIdx);");
        w.println("                  jclass cls = vm_find_class(env, owner);");
        w.println("                  if (!cls) { VM_LOG(\"GETFIELD: Class not found: %s\\n\", owner); frame.pc++; break; }");
        w.println("                  jfieldID fid = vm_get_field_id(env, cls, owner, name, desc);");
        w.println("                  if (!fid) { VM_LOG(\"GETFIELD: Field not found: %s.%s\\n\", owner, name); (*env)->ExceptionClear(env); frame.pc++; break; }");
        w.println("                  char t = desc[0];");
        w.println("                  if (t == 'I' || t == 'B' || t == 'C' || t == 'S' || t == 'Z') {");
        w.println("                      frame.stack[frame.sp++].i = (*env)->GetIntField(env, obj, fid);");
        w.println("                  } else if (t == 'J') {");
        w.println("                      frame.stack[frame.sp++].j = (*env)->GetLongField(env, obj, fid);");
        w.println("                  } else if (t == 'F') {");
        w.println("                      frame.stack[frame.sp++].f = (*env)->GetFloatField(env, obj, fid);");
        w.println("                  } else if (t == 'D') {");
        w.println("                      frame.stack[frame.sp++].d = (*env)->GetDoubleField(env, obj, fid);");
        w.println("                  } else {");
        w.println("                      frame.stack[frame.sp++].l = (*env)->GetObjectField(env, obj, fid);");
        w.println("                  } }");
        pcIncBreak(w);
    }

    @Override
    public void generateComputedGoto(PrintWriter w) {
        w.printf("        OP_%02x:  /* %s */\n", opcode, comment);
        w.println("            { if (UNLIKELY(!meta)) { VM_LOG(\"GETFIELD: meta is NULL at pc=%d\\n\", frame.pc); frame.pc++; DISPATCH_NEXT; }");
        w.println("              jobject obj = frame.stack[--frame.sp].l;");
        w.println("              if (UNLIKELY(!obj)) {");
        w.println("                  jclass npeClass = vm_find_class(env, \"java/lang/NullPointerException\");");
        w.println("                  if (npeClass) (*env)->ThrowNew(env, npeClass, \"\");");
        w.println("                  goto method_exit;");
        w.println("              }");
        w.println("              const char* owner = vm_get_string(meta->ownerIdx);");
        w.println("              const char* name = vm_get_string(meta->nameIdx);");
        w.println("              const char* desc = vm_get_string(meta->descIdx);");
        w.println("              jclass cls = vm_find_class(env, owner);");
        w.println("              if (UNLIKELY(!cls)) { VM_LOG(\"GETFIELD: Class not found: %s\\n\", owner); frame.pc++; DISPATCH_NEXT; }");
        w.println("              jfieldID fid = vm_get_field_id(env, cls, owner, name, desc);");
        w.println("              if (UNLIKELY(!fid)) { VM_LOG(\"GETFIELD: Field not found: %s.%s\\n\", owner, name); (*env)->ExceptionClear(env); frame.pc++; DISPATCH_NEXT; }");
        w.println("              char t = desc[0];");
        w.println("              if (t == 'I' || t == 'B' || t == 'C' || t == 'S' || t == 'Z') {");
        w.println("                  frame.stack[frame.sp++].i = (*env)->GetIntField(env, obj, fid);");
        w.println("              } else if (t == 'J') {");
        w.println("                  frame.stack[frame.sp++].j = (*env)->GetLongField(env, obj, fid);");
        w.println("              } else if (t == 'F') {");
        w.println("                  frame.stack[frame.sp++].f = (*env)->GetFloatField(env, obj, fid);");
        w.println("              } else if (t == 'D') {");
        w.println("                  frame.stack[frame.sp++].d = (*env)->GetDoubleField(env, obj, fid);");
        w.println("              } else {");
        w.println("                  frame.stack[frame.sp++].l = (*env)->GetObjectField(env, obj, fid);");
        w.println("              } }");
        w.println("            frame.pc++;");
        w.println("            DISPATCH_NEXT;");
    }
}
