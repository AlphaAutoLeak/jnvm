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
        w.println("                { const char* owner = vm_strings[meta->ownerIdx].data;");
        w.println("                  const char* name = vm_strings[meta->nameIdx].data;");
        w.println("                  const char* desc = vm_strings[meta->descIdx].data;");
        w.println("                  jclass cls = (*env)->FindClass(env, owner);");
        w.println("                  jfieldID fid = (*env)->GetStaticFieldID(env, cls, name, desc);");
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