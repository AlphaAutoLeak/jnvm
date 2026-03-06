package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import java.io.PrintWriter;

/**
 * Method invocation helper
 */
public class InvokeHelper {
    
    public static void generate(PrintWriter w, boolean isStatic) {
        w.println("                { int invokePc = frame.pc;  // Save PC before invocation");
        w.println("                  if (!meta) { VM_LOG(\"INVOKE: meta is NULL at pc=%d\\n\", frame.pc); frame.pc++; break; }");
        w.println("                  const char* owner = vm_get_string(meta->ownerIdx);");
        w.println("                  const char* name = vm_get_string(meta->nameIdx);");
        w.println("                  const char* desc = vm_get_string(meta->descIdx);");
        w.println("                  jclass cls = vm_find_class(env, owner);");  // 使用缓存版本
        w.println("                  if (!cls) { VM_LOG(\"INVOKE: Class not found: %s\\n\", owner); frame.pc++; break; }");
        if (isStatic) {
            w.println("                  jmethodID mid = vm_get_static_method_id(env, cls, owner, name, desc);");  // 使用缓存版本
        } else {
            w.println("                  jmethodID mid = vm_get_method_id(env, cls, owner, name, desc);");  // 使用缓存版本
        }
        w.println("                  if (!mid) { VM_LOG(\"INVOKE: Method not found: %s.%s%s\\n\", owner, name, desc); (*env)->ExceptionClear(env); frame.pc++; break; }");
        w.println("                  int argCount = 0; char returnType = 'V';");
        w.println("                  vm_parse_method_desc(desc, &argCount, &returnType);");
        w.println("                  jvalue args[16];");
        w.println("                  for (int i = argCount - 1; i >= 0; i--) {");
        w.println("                      char t = vm_get_arg_type(desc, i);");
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
            w.println("                  if (!receiver) {");
            w.println("                      jclass npeClass = vm_find_class(env, \"java/lang/NullPointerException\");");
            w.println("                      if (npeClass) (*env)->ThrowNew(env, npeClass, \"\");");
            w.println("                      goto method_exit;");
            w.println("                  }");
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
            w.println("                          { jint res = (*env)->CallStaticIntMethodA(env, cls, mid, args);");
        } else {
            w.println("                          { jint res = (*env)->CallIntMethodA(env, receiver, mid, args);");
        }
        w.println("                            frame.stack[frame.sp].i = res;");
        w.println("                            frame.stackTypes[frame.sp++] = TYPE_INT; break; }");
        w.println("                      case 'J':");
        if (isStatic) {
            w.println("                          frame.stack[frame.sp].j = (*env)->CallStaticLongMethodA(env, cls, mid, args);");
        } else {
            w.println("                          frame.stack[frame.sp].j = (*env)->CallLongMethodA(env, receiver, mid, args);");
        }
        w.println("                          frame.stackTypes[frame.sp++] = TYPE_LONG; break;");
        w.println("                      case 'F':");
        if (isStatic) {
            w.println("                          frame.stack[frame.sp].f = (*env)->CallStaticFloatMethodA(env, cls, mid, args);");
        } else {
            w.println("                          frame.stack[frame.sp].f = (*env)->CallFloatMethodA(env, receiver, mid, args);");
        }
        w.println("                          frame.stackTypes[frame.sp++] = TYPE_FLOAT; break;");
        w.println("                      case 'D':");
        if (isStatic) {
            w.println("                          frame.stack[frame.sp].d = (*env)->CallStaticDoubleMethodA(env, cls, mid, args);");
        } else {
            w.println("                          frame.stack[frame.sp].d = (*env)->CallDoubleMethodA(env, receiver, mid, args);");
        }
        w.println("                          frame.stackTypes[frame.sp++] = TYPE_DOUBLE; break;");
        w.println("                      default:");
        if (isStatic) {
            w.println("                          { jobject _res = (*env)->CallStaticObjectMethodA(env, cls, mid, args);");
            w.println("                            VM_LOG(\"INVOKE: CallStaticObjectMethodA returned %p\\n\", _res);");
            w.println("                            fflush(stdout);");
            w.println("                            frame.stack[frame.sp].l = _res; }");
        } else {
            w.println("                          { jobject _res = (*env)->CallObjectMethodA(env, receiver, mid, args);");
            w.println("                            VM_LOG(\"INVOKE: CallObjectMethodA returned %p\\n\", _res);");
            w.println("                            fflush(stdout);");
            w.println("                            frame.stack[frame.sp].l = _res; }");
        }
        w.println("                          frame.stackTypes[frame.sp++] = TYPE_REF; break;");
        w.println("                  }");
        // Check exception immediately after method call with correct PC
        w.println("                  if ((*env)->ExceptionCheck(env)) {");
        w.println("                      VM_LOG(\"Exception thrown at pc=%d\\n\", invokePc);");
        w.println("                      jthrowable exc = (*env)->ExceptionOccurred(env);");
        w.println("                      (*env)->ExceptionClear(env);");
        w.println("                      int hPc = vm_find_exception_handler(env, m, invokePc, exc);");
        w.println("                      if (hPc >= 0) {");
        w.println("                          frame.sp = 0;");
        w.println("                          frame.stack[frame.sp++].l = exc;");
        w.println("                          frame.pc = hPc;");
        w.println("                          continue;");
        w.println("                      }");
        w.println("                      VM_LOG(\"No handler found, rethrowing\\n\");");
        w.println("                      (*env)->Throw(env, exc);");
        w.println("                      goto method_exit;");
        w.println("                  }");
        w.println("                }");
        w.println("                frame.pc++;");
    }
}