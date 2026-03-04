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
        w.println("                { jobject obj = frame.stack[--frame.sp].l;");
        w.println("                  const char* owner = vm_strings[meta->ownerIdx].data;");
        w.println("                  const char* name = vm_strings[meta->nameIdx].data;");
        w.println("                  const char* desc = vm_strings[meta->descIdx].data;");
        w.println("                  jclass cls = (*env)->FindClass(env, owner);");
        w.println("                  jfieldID fid = (*env)->GetFieldID(env, cls, name, desc);");
        w.println("                  char t = desc[0];");
        w.println("                  if (t == 'I' || t == 'B' || t == 'C' || t == 'S' || t == 'Z')");
        w.println("                      frame.stack[frame.sp++].i = (*env)->GetIntField(env, obj, fid);");
        w.println("                  else if (t == 'J')");
        w.println("                      frame.stack[frame.sp++].j = (*env)->GetLongField(env, obj, fid);");
        w.println("                  else if (t == 'F')");
        w.println("                      frame.stack[frame.sp++].f = (*env)->GetFloatField(env, obj, fid);");
        w.println("                  else if (t == 'D')");
        w.println("                      frame.stack[frame.sp++].d = (*env)->GetDoubleField(env, obj, fid);");
        w.println("                  else");
        w.println("                      frame.stack[frame.sp++].l = (*env)->GetObjectField(env, obj, fid); }");
        pcIncBreak(w);
    }
}