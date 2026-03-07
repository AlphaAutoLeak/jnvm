package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import java.io.PrintWriter;

/**
 * Method invocation helper (64-bit only)
 * Supports direct VM-to-VM calls to bypass JNI boxing/unboxing overhead
 */
public class InvokeHelper {

    public static void generate(PrintWriter w, boolean isStatic) {
        generate(w, isStatic, true);
    }

    public static void generate(PrintWriter w, boolean isStatic, boolean directCallEnabled) {
        w.println("                { int invokePc = frame.pc;");
        w.println("                  if (!meta) { VM_LOG(\"INVOKE: meta is NULL at pc=%d\\n\", frame.pc); frame.pc++; break; }");
        w.println("                  const char* owner = vm_get_string(meta->ownerIdx);");
        w.println("                  const char* name = vm_get_string(meta->nameIdx);");
        w.println("                  const char* desc = vm_get_string(meta->descIdx);");
        // Use pre-computed metadata
        w.println("                  int argCount = meta->argCount;");
        w.println("                  char returnType = meta->returnTypeChar;");
        w.println("                  const char* argTypes = (meta->argTypesIdx >= 0) ? vm_get_string(meta->argTypesIdx) : NULL;");

        if (directCallEnabled) {
            // Direct VM-to-VM call path
            w.println("                  int vmTargetId = meta->vmTargetId;");
            w.println("                  if (vmTargetId >= 0) {");
            emitDirectCallBody(w, isStatic, false);
            w.println("                  } else {");
        }

        // Original JNI path (with lazy cached method ID)
        w.println("                  jclass cls; jmethodID mid;");
        w.println("                  if (meta->cachedMid != NULL) {");
        w.println("                      cls = meta->cachedClass;");
        w.println("                      mid = meta->cachedMid;");
        w.println("                  } else {");
        w.println("                  cls = vm_find_class(env, owner);");
        w.println("                  if (!cls) { VM_LOG(\"INVOKE: Class not found: %s\\n\", owner); frame.pc++; break; }");
        if (isStatic) {
            w.println("                  mid = vm_get_static_method_id(env, cls, owner, name, desc);");
        } else {
            w.println("                  mid = vm_get_method_id(env, cls, owner, name, desc);");
        }
        w.println("                  if (!mid) { VM_LOG(\"INVOKE: Method not found: %s.%s%s\\n\", owner, name, desc); (*env)->ExceptionClear(env); frame.pc++; break; }");
        w.println("                  meta->cachedClass = cls;");
        w.println("                  meta->cachedMid = mid;");
        w.println("                  }");
        w.println("                  jvalue args[16];");
        w.println("                  for (int i = argCount - 1; i >= 0; i--) {");
        w.println("                      char t = argTypes ? argTypes[i] : 'L';");
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
            w.println("                      _hasException = 1; goto method_exit;");
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
        w.println("                      _hasException = 1; goto method_exit;");
        w.println("                  }");
        if (directCallEnabled) {
            w.println("                  }"); // end else (JNI path)
        }
        w.println("                }");
        w.println("                frame.pc++;");
    }

    /**
     * Generate computed goto version
     */
    public static void generateComputedGoto(PrintWriter w, boolean isStatic, int opcode, String comment) {
        generateComputedGoto(w, isStatic, opcode, comment, true);
    }

    public static void generateComputedGoto(PrintWriter w, boolean isStatic, int opcode, String comment, boolean directCallEnabled) {
        w.printf("        OP_%02x:  /* %s */\n", opcode, comment);
        w.println("            { int invokePc = frame.pc;");
        w.println("              if (UNLIKELY(!meta)) { VM_LOG(\"INVOKE: meta is NULL at pc=%d\\n\", frame.pc); frame.pc++; DISPATCH_NEXT; }");
        w.println("              const char* owner = vm_get_string(meta->ownerIdx);");
        w.println("              const char* name = vm_get_string(meta->nameIdx);");
        w.println("              const char* desc = vm_get_string(meta->descIdx);");
        // Use pre-computed metadata
        w.println("              int argCount = meta->argCount;");
        w.println("              char returnType = meta->returnTypeChar;");
        w.println("              const char* argTypes = (meta->argTypesIdx >= 0) ? vm_get_string(meta->argTypesIdx) : NULL;");

        if (directCallEnabled) {
            // Direct VM-to-VM call path
            w.println("              int vmTargetId = meta->vmTargetId;");
            w.println("              if (vmTargetId >= 0) {");
            emitDirectCallBody(w, isStatic, true);
            w.println("              } else {");
        }

        // Original JNI path (with lazy cached method ID)
        w.println("              jclass cls; jmethodID mid;");
        w.println("              if (LIKELY(meta->cachedMid != NULL)) {");
        w.println("                  cls = meta->cachedClass;");
        w.println("                  mid = meta->cachedMid;");
        w.println("              } else {");
        w.println("                  cls = vm_find_class(env, owner);");
        w.println("                  if (UNLIKELY(!cls)) { VM_LOG(\"INVOKE: Class not found: %s\\n\", owner); frame.pc++; DISPATCH_NEXT; }");
        if (isStatic) {
            w.println("                  mid = vm_get_static_method_id(env, cls, owner, name, desc);");
        } else {
            w.println("                  mid = vm_get_method_id(env, cls, owner, name, desc);");
        }
        w.println("                  if (UNLIKELY(!mid)) { VM_LOG(\"INVOKE: Method not found: %s.%s%s\\n\", owner, name, desc); (*env)->ExceptionClear(env); frame.pc++; DISPATCH_NEXT; }");
        w.println("                  meta->cachedClass = cls;");
        w.println("                  meta->cachedMid = mid;");
        w.println("              }");
        w.println("              jvalue args[16];");
        w.println("              for (int i = argCount - 1; i >= 0; i--) {");
        w.println("                  char t = argTypes ? argTypes[i] : 'L';");
        w.println("                  switch (t) {");
        w.println("                      case 'I': case 'B': case 'C': case 'S': case 'Z':");
        w.println("                          args[i].i = frame.stack[--frame.sp].i; break;");
        w.println("                      case 'J': args[i].j = frame.stack[--frame.sp].j; break;");
        w.println("                      case 'F': args[i].f = frame.stack[--frame.sp].f; break;");
        w.println("                      case 'D': args[i].d = frame.stack[--frame.sp].d; break;");
        w.println("                      default: args[i].l = frame.stack[--frame.sp].l; break;");
        w.println("                  }");
        w.println("              }");

        if (!isStatic) {
            w.println("              jobject receiver = frame.stack[--frame.sp].l;");
            w.println("              if (UNLIKELY(!receiver)) {");
            w.println("                  jclass npeClass = vm_find_class(env, \"java/lang/NullPointerException\");");
            w.println("                  if (npeClass) (*env)->ThrowNew(env, npeClass, \"\");");
            w.println("                  _hasException = 1; goto method_exit;");
            w.println("              }");
        }

        w.println("              switch (returnType) {");
        w.println("                  case 'V':");
        if (isStatic) {
            w.println("                      (*env)->CallStaticVoidMethodA(env, cls, mid, args); break;");
        } else {
            w.println("                      (*env)->CallVoidMethodA(env, receiver, mid, args); break;");
        }
        w.println("                  case 'I': case 'B': case 'C': case 'S': case 'Z':");
        if (isStatic) {
            w.println("                      frame.stack[frame.sp++].i = (*env)->CallStaticIntMethodA(env, cls, mid, args); break;");
        } else {
            w.println("                      frame.stack[frame.sp++].i = (*env)->CallIntMethodA(env, receiver, mid, args); break;");
        }
        w.println("                  case 'J':");
        if (isStatic) {
            w.println("                      frame.stack[frame.sp++].j = (*env)->CallStaticLongMethodA(env, cls, mid, args); break;");
        } else {
            w.println("                      frame.stack[frame.sp++].j = (*env)->CallLongMethodA(env, receiver, mid, args); break;");
        }
        w.println("                  case 'F':");
        if (isStatic) {
            w.println("                      frame.stack[frame.sp++].f = (*env)->CallStaticFloatMethodA(env, cls, mid, args); break;");
        } else {
            w.println("                      frame.stack[frame.sp++].f = (*env)->CallFloatMethodA(env, receiver, mid, args); break;");
        }
        w.println("                  case 'D':");
        if (isStatic) {
            w.println("                      frame.stack[frame.sp++].d = (*env)->CallStaticDoubleMethodA(env, cls, mid, args); break;");
        } else {
            w.println("                      frame.stack[frame.sp++].d = (*env)->CallDoubleMethodA(env, receiver, mid, args); break;");
        }
        w.println("                  default:");
        if (isStatic) {
            w.println("                      frame.stack[frame.sp++].l = (*env)->CallStaticObjectMethodA(env, cls, mid, args); break;");
        } else {
            w.println("                      frame.stack[frame.sp++].l = (*env)->CallObjectMethodA(env, receiver, mid, args); break;");
        }
        w.println("              }");
        w.println("              if (UNLIKELY((*env)->ExceptionCheck(env))) {");
        w.println("                  VM_LOG(\"Exception thrown at pc=%d\\n\", invokePc);");
        w.println("                  jthrowable exc = (*env)->ExceptionOccurred(env);");
        w.println("                  (*env)->ExceptionClear(env);");
        w.println("                  int hPc = vm_find_exception_handler(env, m, invokePc, exc);");
        w.println("                  if (hPc >= 0) {");
        w.println("                      frame.sp = 0;");
        w.println("                      frame.stack[frame.sp++].l = exc;");
        w.println("                      frame.pc = hPc;");
        w.println("                      DISPATCH_NEXT;");
        w.println("                  }");
        w.println("                  VM_LOG(\"No handler found, rethrowing\\n\");");
        w.println("                  (*env)->Throw(env, exc);");
        w.println("                  _hasException = 1; goto method_exit;");
        w.println("              }");
        if (directCallEnabled) {
            w.println("              }"); // end else (JNI path)
        }
        w.println("            }");
        w.println("            frame.pc++;");
        w.println("            DISPATCH_NEXT;");
    }

    /**
     * Emit the direct VM-to-VM call body (shared by switch and computed goto versions)
     * Pops args from caller stack, builds callee locals, calls vm_execute_common directly.
     */
    private static void emitDirectCallBody(PrintWriter w, boolean isStatic, boolean computedGoto) {
        String indent = computedGoto ? "                  " : "                      ";
        String dispatchOrContinue = computedGoto ? "DISPATCH_NEXT" : "continue";

        // Build callee locals directly from caller's stack
        w.println(indent + "int targetMaxLocals = vm_methods[vmTargetId].maxLocals;");
        w.println(indent + "VMValue tempLocals[targetMaxLocals];");
        w.println(indent + "memset(tempLocals, 0, targetMaxLocals * sizeof(VMValue));");

        // Calculate local slot positions for each argument
        w.println(indent + "int slotMap[16];");
        if (isStatic) {
            w.println(indent + "int nextSlot = 0;");
        } else {
            w.println(indent + "int nextSlot = 1;");
        }
        w.println(indent + "for (int i = 0; i < argCount; i++) {");
        w.println(indent + "    slotMap[i] = nextSlot;");
        w.println(indent + "    nextSlot++;");
        w.println(indent + "    char t = argTypes ? argTypes[i] : 'L';");
        w.println(indent + "    if (t == 'J' || t == 'D') nextSlot++;");
        w.println(indent + "}");

        // Pop args from caller stack (reverse order) into callee locals
        w.println(indent + "for (int i = argCount - 1; i >= 0; i--) {");
        w.println(indent + "    tempLocals[slotMap[i]] = frame.stack[--frame.sp];");
        w.println(indent + "}");

        if (!isStatic) {
            // Pop receiver and set in local 0
            w.println(indent + "jobject directReceiver = frame.stack[--frame.sp].l;");
            w.println(indent + "if (UNLIKELY(!directReceiver)) {");
            w.println(indent + "    jclass npeClass = vm_find_class(env, \"java/lang/NullPointerException\");");
            w.println(indent + "    if (npeClass) (*env)->ThrowNew(env, npeClass, \"\");");
            w.println(indent + "    _hasException = 1; goto method_exit;");
            w.println(indent + "}");
            w.println(indent + "tempLocals[0].l = directReceiver;");
        }

        // Call vm_execute_common directly with pre-built locals
        w.println(indent + "int obfTargetId = vmTargetId ^ METHOD_ID_XOR_KEY;");
        w.println(indent + "ExecuteResult directResult = vm_execute_common(env, obfTargetId, NULL, NULL, frame.callerClass, tempLocals, targetMaxLocals);");

        // Check for exceptions from direct call (use returnType flag, no JNI ExceptionCheck needed)
        w.println(indent + "if (UNLIKELY(directResult.returnType == 'X')) {");
        w.println(indent + "    jthrowable exc = (*env)->ExceptionOccurred(env);");
        w.println(indent + "    (*env)->ExceptionClear(env);");
        w.println(indent + "    int hPc = vm_find_exception_handler(env, m, invokePc, exc);");
        w.println(indent + "    if (hPc >= 0) {");
        w.println(indent + "        frame.sp = 0;");
        w.println(indent + "        frame.stack[frame.sp++].l = exc;");
        w.println(indent + "        frame.pc = hPc;");
        w.println(indent + "        " + dispatchOrContinue + ";");
        w.println(indent + "    }");
        w.println(indent + "    (*env)->Throw(env, exc);");
        w.println(indent + "    _hasException = 1;");
        w.println(indent + "    goto method_exit;");
        w.println(indent + "}");

        // Push result based on return type
        w.println(indent + "switch (returnType) {");
        w.println(indent + "    case 'V': break;");
        w.println(indent + "    case 'I': case 'B': case 'C': case 'S': case 'Z':");
        w.println(indent + "        frame.stack[frame.sp++].i = directResult.value.i; break;");
        w.println(indent + "    case 'J': frame.stack[frame.sp++].j = directResult.value.j; break;");
        w.println(indent + "    case 'F': frame.stack[frame.sp++].f = directResult.value.f; break;");
        w.println(indent + "    case 'D': frame.stack[frame.sp++].d = directResult.value.d; break;");
        w.println(indent + "    default: frame.stack[frame.sp++].l = directResult.value.l; break;");
        w.println(indent + "}");
    }
}
