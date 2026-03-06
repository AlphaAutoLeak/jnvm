package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * GETSTATIC instruction (64-bit only)
 */
public class GetStaticInstruction extends Instruction {
    public GetStaticInstruction() {
        super(0xb2, "GETSTATIC");
    }
    
    @Override
    public boolean needsMeta() {
        return true;  // 需要字段信息
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { const char* owner = vm_get_string(meta->ownerIdx);");
        w.println("                  const char* name = vm_get_string(meta->nameIdx);");
        w.println("                  const char* desc = vm_get_string(meta->descIdx);");
        w.println("                  jclass cls = vm_find_class(env, owner);");
        w.println("                  if (!cls) { frame.pc++; break; }");
        w.println("                  jfieldID fid = vm_get_static_field_id(env, cls, owner, name, desc);");
        w.println("                  if (!fid) { (*env)->ExceptionClear(env); frame.pc++; break; }");
        w.println("                  char t = desc[0];");
        w.println("                  if (t == 'I' || t == 'B' || t == 'C' || t == 'S' || t == 'Z') {");
        w.println("                      frame.stack[frame.sp++].i = (*env)->GetStaticIntField(env, cls, fid);");
        w.println("                  } else if (t == 'J') {");
        w.println("                      frame.stack[frame.sp++].j = (*env)->GetStaticLongField(env, cls, fid);");
        w.println("                  } else if (t == 'F') {");
        w.println("                      frame.stack[frame.sp++].f = (*env)->GetStaticFloatField(env, cls, fid);");
        w.println("                  } else if (t == 'D') {");
        w.println("                      frame.stack[frame.sp++].d = (*env)->GetStaticDoubleField(env, cls, fid);");
        w.println("                  } else {");
        w.println("                      frame.stack[frame.sp++].l = (*env)->GetStaticObjectField(env, cls, fid);");
        w.println("                  }");
        w.println("                }");
        w.println("                frame.pc++;");
    }

    @Override
    public void generateComputedGoto(PrintWriter w) {
        w.printf("        OP_%02x:  /* %s */\n", opcode, comment);
        w.println("            { const char* owner = vm_get_string(meta->ownerIdx);");
        w.println("              const char* name = vm_get_string(meta->nameIdx);");
        w.println("              const char* desc = vm_get_string(meta->descIdx);");
        w.println("              jclass cls = vm_find_class(env, owner);");
        w.println("              if (!cls) { frame.pc++; DISPATCH_NEXT; }");
        w.println("              jfieldID fid = vm_get_static_field_id(env, cls, owner, name, desc);");
        w.println("              if (!fid) { (*env)->ExceptionClear(env); frame.pc++; DISPATCH_NEXT; }");
        w.println("              char t = desc[0];");
        w.println("              if (t == 'I' || t == 'B' || t == 'C' || t == 'S' || t == 'Z') {");
        w.println("                  frame.stack[frame.sp++].i = (*env)->GetStaticIntField(env, cls, fid);");
        w.println("              } else if (t == 'J') {");
        w.println("                  frame.stack[frame.sp++].j = (*env)->GetStaticLongField(env, cls, fid);");
        w.println("              } else if (t == 'F') {");
        w.println("                  frame.stack[frame.sp++].f = (*env)->GetStaticFloatField(env, cls, fid);");
        w.println("              } else if (t == 'D') {");
        w.println("                  frame.stack[frame.sp++].d = (*env)->GetStaticDoubleField(env, cls, fid);");
        w.println("              } else {");
        w.println("                  frame.stack[frame.sp++].l = (*env)->GetStaticObjectField(env, cls, fid);");
        w.println("              } }");
        w.println("            frame.pc++;");
        w.println("            DISPATCH_NEXT;");
    }
}
