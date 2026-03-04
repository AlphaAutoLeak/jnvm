package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * PUTFIELD 指令
 */
public class PutFieldInstruction extends Instruction {
    public PutFieldInstruction() {
        super(0xb5, "PUTFIELD");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { VMValue val = frame.stack[--frame.sp];");
        w.println("                  jobject obj = frame.stack[--frame.sp].l;");
        w.println("                  const char* owner = vm_strings[meta->ownerIdx].data;");
        w.println("                  const char* name = vm_strings[meta->nameIdx].data;");
        w.println("                  const char* desc = vm_strings[meta->descIdx].data;");
        w.println("                  jclass cls = (*env)->FindClass(env, owner);");
        w.println("                  jfieldID fid = (*env)->GetFieldID(env, cls, name, desc);");
        w.println("                  char t = desc[0];");
        w.println("                  if (t == 'I' || t == 'B' || t == 'C' || t == 'S' || t == 'Z')");
        w.println("                      (*env)->SetIntField(env, obj, fid, val.i);");
        w.println("                  else if (t == 'J')");
        w.println("                      (*env)->SetLongField(env, obj, fid, val.j);");
        w.println("                  else if (t == 'F')");
        w.println("                      (*env)->SetFloatField(env, obj, fid, val.f);");
        w.println("                  else if (t == 'D')");
        w.println("                      (*env)->SetDoubleField(env, obj, fid, val.d);");
        w.println("                  else");
        w.println("                      (*env)->SetObjectField(env, obj, fid, val.l); }");
        pcIncBreak(w);
    }
}
