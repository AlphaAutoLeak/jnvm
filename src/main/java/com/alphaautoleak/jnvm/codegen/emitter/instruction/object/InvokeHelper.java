package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import java.io.PrintWriter;

/**
 * 方法调用指令辅助�? */
public class InvokeHelper {
    
    public static void generate(PrintWriter w, boolean isStatic) {
        w.println("                { const char* owner = vm_strings[meta->ownerIdx].data;");
        w.println("                  const char* name = vm_strings[meta->nameIdx].data;");
        w.println("                  const char* desc = vm_strings[meta->descIdx].data;");
        w.println("                  jclass cls = (*env)->FindClass(env, owner);");
        if (isStatic) {
            w.println("                  jmethodID mid = (*env)->GetStaticMethodID(env, cls, name, desc);");
        } else {
            w.println("                  jmethodID mid = (*env)->GetMethodID(env, cls, name, desc);");
        }
        w.println("                  int argCount = 0; char returnType = 'V';");
        w.println("                  parse_method_desc(desc, &argCount, &returnType);");
        w.println("                  jvalue args[16];");
        w.println("                  for (int i = argCount - 1; i >= 0; i--) {");
        w.println("                      char t = desc[1 + i];");
        w.println("                      switch (t) {");
        w.println("                          case 'I': case 'B': case 'C': case 'S': case 'Z':");
        w.println("                              args[i].i = frame.stack[--frame.sp].i; break;");
        w.println("                          case 'J': args[i].j = frame.stack[--frame.sp].j; break;");
        w.println("                          case 'F': args[i].f = frame.stack[--frame.sp].f; break;");
        w.println("                          case 'D': args[i].d = frame.stack[--frame.sp].d; break;");
        w.println("                          default: args[i].l = frame.stack[--frame.sp].l; break;");
        w.println("                      }");
        w.println("                  }");
        
        if (!isStatic) {
            w.println("                  jobject receiver = frame.stack[--frame.sp].l;");
        }
        
        w.println("                  switch (returnType) {");
        w.println("                      case 'V':");
        if (isStatic) {
            w.println("                          (*env)->CallStaticVoidMethodA(env, cls, mid, args); break;");
        } else {
            w.println("                          (*env)->CallVoidMethodA(env, receiver, mid, args); break;");
        }
        w.println("                      case 'I': case 'B': case 'C': case 'S': case 'Z':");
        if (isStatic) {
            w.println("                          frame.stack[frame.sp++].i = (*env)->CallStaticIntMethodA(env, cls, mid, args); break;");
        } else {
            w.println("                          frame.stack[frame.sp++].i = (*env)->CallIntMethodA(env, receiver, mid, args); break;");
        }
        w.println("                      case 'J':");
        if (isStatic) {
            w.println("                          frame.stack[frame.sp++].j = (*env)->CallStaticLongMethodA(env, cls, mid, args); break;");
        } else {
            w.println("                          frame.stack[frame.sp++].j = (*env)->CallLongMethodA(env, receiver, mid, args); break;");
        }
        w.println("                      case 'F':");
        if (isStatic) {
            w.println("                          frame.stack[frame.sp++].f = (*env)->CallStaticFloatMethodA(env, cls, mid, args); break;");
        } else {
            w.println("                          frame.stack[frame.sp++].f = (*env)->CallFloatMethodA(env, receiver, mid, args); break;");
        }
        w.println("                      case 'D':");
        if (isStatic) {
            w.println("                          frame.stack[frame.sp++].d = (*env)->CallStaticDoubleMethodA(env, cls, mid, args); break;");
        } else {
            w.println("                          frame.stack[frame.sp++].d = (*env)->CallDoubleMethodA(env, receiver, mid, args); break;");
        }
        w.println("                      default:");
        if (isStatic) {
            w.println("                          frame.stack[frame.sp++].l = (*env)->CallStaticObjectMethodA(env, cls, mid, args); break;");
        } else {
            w.println("                          frame.stack[frame.sp++].l = (*env)->CallObjectMethodA(env, receiver, mid, args); break;");
        }
        w.println("                  }");
        w.println("                }");
        w.println("                frame.pc++;");
    }
}
