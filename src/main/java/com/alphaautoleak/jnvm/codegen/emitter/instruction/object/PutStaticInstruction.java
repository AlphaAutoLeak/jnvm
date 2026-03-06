package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * PUTSTATIC instruction
 */
public class PutStaticInstruction extends Instruction {
    public PutStaticInstruction() {
        super(0xb3, "PUTSTATIC");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { const char* owner = vm_get_string(meta->ownerIdx);");
        w.println("                  const char* name = vm_get_string(meta->nameIdx);");
        w.println("                  const char* desc = vm_get_string(meta->descIdx);");
        w.println("                  jclass cls = vm_find_class(env, owner);");  // 使用缓存版本
        w.println("                  if (!cls) { frame.pc++; break; }");
        w.println("                  jfieldID fid = (*env)->GetStaticFieldID(env, cls, name, desc);");
        w.println("                  if (!fid) { (*env)->ExceptionClear(env); frame.pc++; break; }");
        w.println("                  char t = desc[0];");
        w.println("                  if (t == 'I' || t == 'B' || t == 'C' || t == 'S' || t == 'Z')");
        w.println("                      (*env)->SetStaticIntField(env, cls, fid, frame.stack[--frame.sp].i);");
        w.println("                  else if (t == 'J')");
        w.println("                      (*env)->SetStaticLongField(env, cls, fid, frame.stack[--frame.sp].j);");
        w.println("                  else if (t == 'F')");
        w.println("                      (*env)->SetStaticFloatField(env, cls, fid, frame.stack[--frame.sp].f);");
        w.println("                  else if (t == 'D')");
        w.println("                      (*env)->SetStaticDoubleField(env, cls, fid, frame.stack[--frame.sp].d);");
        w.println("                  else");
        w.println("                      (*env)->SetStaticObjectField(env, cls, fid, frame.stack[--frame.sp].l); }");
        pcIncBreak(w);
    }
}