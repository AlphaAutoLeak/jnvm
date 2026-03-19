package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import java.io.PrintWriter;

/**
 * Method invocation helper (64-bit only)
 * Supports direct VM-to-VM calls to bypass JNI boxing/unboxing overhead
 */
public class InvokeHelper {

    public static void generate(PrintWriter w, boolean isStatic) {
        generate(w, isStatic, false);
    }

    public static void generate(PrintWriter w, boolean isStatic, boolean directCallEnabled) {
        generate(w, isStatic, directCallEnabled, false);
    }

    public static void generate(PrintWriter w, boolean isStatic, boolean directCallEnabled, boolean requireExactReceiverOwner) {
        w.println("                { int invokePc = frame.pc;");
        w.println("                  if (!meta) { VM_LOG(\"INVOKE: meta is NULL at pc=%d\\n\", frame.pc); frame.pc++; break; }");
        w.println("                  const char* owner = meta->ownerStr ? meta->ownerStr : vm_get_string(meta->ownerIdx);");
        w.println("                  const char* name = meta->nameStr ? meta->nameStr : vm_get_string(meta->nameIdx);");
        w.println("                  const char* desc = meta->descStr ? meta->descStr : vm_get_string(meta->descIdx);");
        // Use pre-computed metadata
        w.println("                  int argCount = meta->argCount;");
        w.println("                  char returnType = meta->returnTypeChar;");
        w.println("                  const char* argTypes = meta->argTypesStr ? meta->argTypesStr : ((meta->argTypesIdx >= 0) ? vm_get_string(meta->argTypesIdx) : NULL);");
        w.println("                  VM_PERF_INVOKE_BEGIN();");

        if (directCallEnabled) {
            // Direct VM-to-VM call path
            w.println("                  int vmTargetId = meta->vmTargetId;");
            w.println("                  if (vmTargetId >= 0) { VM_PERF_DIRECT_CANDIDATE(); }");
            if (!isStatic && requireExactReceiverOwner) {
                w.println("                      int _canDirectCall = 0;");
                w.println("                      if (LIKELY(vmTargetId >= 0 && frame.sp > argCount)) {");
                w.println("                          jobject _peekReceiver = frame.stack[frame.sp - argCount - 1].l;");
                w.println("                          if (_peekReceiver != NULL) {");
                w.println("                              jclass _recvCls = (*env)->GetObjectClass(env, _peekReceiver);");
                w.println("                              if (_recvCls != NULL) {");
                w.println("                                  if (meta->directRecvExactClass != NULL && (*env)->IsSameObject(env, _recvCls, meta->directRecvExactClass)) {");
                w.println("                                      _canDirectCall = 1;");
                w.println("                                  } else {");
                w.println("                                      jclass _ownerCls = meta->directOwnerClass;");
                w.println("                                      if (_ownerCls == NULL) {");
                w.println("                                          _ownerCls = vm_find_class(env, owner);");
                w.println("                                          if (_ownerCls != NULL) meta->directOwnerClass = _ownerCls;");
                w.println("                                      }");
                w.println("                                      if (_ownerCls != NULL && (*env)->IsSameObject(env, _recvCls, _ownerCls)) {");
                w.println("                                          _canDirectCall = 1;");
                w.println("                                          if (meta->directRecvExactClass == NULL) {");
                w.println("                                              meta->directRecvExactClass = (*env)->NewGlobalRef(env, _recvCls);");
                w.println("                                          }");
                w.println("                                      }");
                w.println("                                  }");
                w.println("                                  (*env)->DeleteLocalRef(env, _recvCls);");
                w.println("                              }");
                w.println("                          }");
                w.println("                      }");
                w.println("                      if (vmTargetId >= 0 && _canDirectCall) {");
                emitDirectCallBody(w, isStatic, false);
                w.println("                      } else {");
                w.println("                      if (vmTargetId >= 0 && !_canDirectCall) { VM_PERF_DIRECT_REJECT(); }");
            } else {
                w.println("                  if (vmTargetId >= 0) {");
                emitDirectCallBody(w, isStatic, false);
                w.println("                  } else {");
            }
        }

        // Original JNI path (with lazy cached method ID)
        w.println("                  VM_PERF_JNI_PATH();");
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
        emitBuildJniArgsFromStack(w, "                  ");

        if (!isStatic) {
            w.println("                  jobject receiver = frame.stack[--frame.sp].l;");
        w.println("                  if (!receiver) {");
        w.println("                      jclass npeClass = vm_find_class(env, \"java/lang/NullPointerException\");");
        w.println("                      if (npeClass) (*env)->ThrowNew(env, npeClass, \"\");");
        w.println("                      _hasException = 1; goto method_exit;");
        w.println("                  }");
        }
        emitMethodHandleInvokeSpecialCase(w, isStatic, false);

        w.println("                  #if VM_DEBUG_ENABLED");
        w.println("                  const char* _cowner = vm_get_string(m->ownerIdx);");
        w.println("                  const char* _cname  = vm_get_string(m->nameIdx);");
        w.println("                  const char* _cdesc  = vm_get_string(m->descIdx);");
        w.println("                  VM_LOG(\"INVOKE FROM %s.%s%s pc=%d -> %s.%s%s argc=%d\\n\", _cowner, _cname, _cdesc, invokePc, owner, name, desc, argCount);");
        w.println("                  if (argTypes) VM_LOG(\"INVOKE ARGTYPES %s\\n\", argTypes);");
        if (!isStatic) {
            w.println("                  if (owner && name && desc &&");
            w.println("                      strcmp(owner, \"java/lang/invoke/MethodHandle\") == 0 &&");
            w.println("                      strcmp(name, \"asCollector\") == 0 &&");
            w.println("                      strcmp(desc, \"(Ljava/lang/Class;I)Ljava/lang/invoke/MethodHandle;\") == 0) {");
            w.println("                      VM_LOG(\"asCollector recv=%p arg0=%p arg1=%d sp=%d\\n\", (void*)receiver, (void*)args[0].l, args[1].i, frame.sp);");
            w.println("                  }");
        }
        w.println("                  #endif");
        w.println("                  VM_LOG_FLUSH();");

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
        w.println("                      const char* _owner = vm_get_string(m->ownerIdx);");
        w.println("                      const char* _name  = vm_get_string(m->nameIdx);");
        w.println("                      const char* _desc  = vm_get_string(m->descIdx);");
        w.println("                      VM_LOG(\"Exception thrown at pc=%d in %s.%s%s\\n\", invokePc, _owner, _name, _desc);");
        w.println("                      VM_LOG(\"Invoke target: %s.%s%s\\n\", owner, name, desc);");
        if (!isStatic) {
            w.println("                      if (owner && name && desc &&");
            w.println("                          strcmp(owner, \"java/lang/String\") == 0 &&");
            w.println("                          strcmp(name, \"substring\") == 0 &&");
            w.println("                          strcmp(desc, \"(II)Ljava/lang/String;\") == 0) {");
            w.println("                          VM_LOG(\"substring args: begin=%d, end=%d\\n\", args[0].i, args[1].i);");
            w.println("                          if (receiver) VM_LOG(\"substring recv len=%d\\n\", (*env)->GetStringLength(env, (jstring)receiver));");
            w.println("                      }");
        }
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
        generateComputedGoto(w, isStatic, opcode, comment, false);
    }

    public static void generateComputedGoto(PrintWriter w, boolean isStatic, int opcode, String comment, boolean directCallEnabled) {
        generateComputedGoto(w, isStatic, opcode, comment, directCallEnabled, false);
    }

    public static void generateComputedGoto(PrintWriter w, boolean isStatic, int opcode, String comment, boolean directCallEnabled, boolean requireExactReceiverOwner) {
        w.printf("        OP_%02x:  /* %s */\n", opcode, comment);
        w.println("            { int invokePc = frame.pc;");
        w.println("              if (UNLIKELY(!meta)) { VM_LOG(\"INVOKE: meta is NULL at pc=%d\\n\", frame.pc); frame.pc++; DISPATCH_NEXT; }");
        w.println("              const char* owner = meta->ownerStr ? meta->ownerStr : vm_get_string(meta->ownerIdx);");
        w.println("              const char* name = meta->nameStr ? meta->nameStr : vm_get_string(meta->nameIdx);");
        w.println("              const char* desc = meta->descStr ? meta->descStr : vm_get_string(meta->descIdx);");
        // Use pre-computed metadata
        w.println("              int argCount = meta->argCount;");
        w.println("              char returnType = meta->returnTypeChar;");
        w.println("              const char* argTypes = meta->argTypesStr ? meta->argTypesStr : ((meta->argTypesIdx >= 0) ? vm_get_string(meta->argTypesIdx) : NULL);");
        w.println("              VM_PERF_INVOKE_BEGIN();");

        if (directCallEnabled) {
            // Direct VM-to-VM call path
            w.println("              int vmTargetId = meta->vmTargetId;");
            w.println("              if (vmTargetId >= 0) { VM_PERF_DIRECT_CANDIDATE(); }");
            if (!isStatic && requireExactReceiverOwner) {
                w.println("                  int _canDirectCall = 0;");
                w.println("                  if (LIKELY(vmTargetId >= 0 && frame.sp > argCount)) {");
                w.println("                      jobject _peekReceiver = frame.stack[frame.sp - argCount - 1].l;");
                w.println("                      if (_peekReceiver != NULL) {");
                w.println("                          jclass _recvCls = (*env)->GetObjectClass(env, _peekReceiver);");
                w.println("                          if (_recvCls != NULL) {");
                w.println("                              if (meta->directRecvExactClass != NULL && (*env)->IsSameObject(env, _recvCls, meta->directRecvExactClass)) {");
                w.println("                                  _canDirectCall = 1;");
                w.println("                              } else {");
                w.println("                                  jclass _ownerCls = meta->directOwnerClass;");
                w.println("                                  if (_ownerCls == NULL) {");
                w.println("                                      _ownerCls = vm_find_class(env, owner);");
                w.println("                                      if (_ownerCls != NULL) meta->directOwnerClass = _ownerCls;");
                w.println("                                  }");
                w.println("                                  if (_ownerCls != NULL && (*env)->IsSameObject(env, _recvCls, _ownerCls)) {");
                w.println("                                      _canDirectCall = 1;");
                w.println("                                      if (meta->directRecvExactClass == NULL) {");
                w.println("                                          meta->directRecvExactClass = (*env)->NewGlobalRef(env, _recvCls);");
                w.println("                                      }");
                w.println("                                  }");
                w.println("                              }");
                w.println("                              (*env)->DeleteLocalRef(env, _recvCls);");
                w.println("                          }");
                w.println("                      }");
                w.println("                  }");
                w.println("                  if (vmTargetId >= 0 && _canDirectCall) {");
                emitDirectCallBody(w, isStatic, true);
                w.println("                  } else {");
                w.println("                  if (vmTargetId >= 0 && !_canDirectCall) { VM_PERF_DIRECT_REJECT(); }");
            } else {
                w.println("              if (vmTargetId >= 0) {");
                emitDirectCallBody(w, isStatic, true);
                w.println("              } else {");
            }
        }

        // Original JNI path (with lazy cached method ID)
        w.println("              VM_PERF_JNI_PATH();");
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
        emitBuildJniArgsFromStack(w, "              ");

        if (!isStatic) {
            w.println("              jobject receiver = frame.stack[--frame.sp].l;");
            w.println("              if (UNLIKELY(!receiver)) {");
            w.println("                  jclass npeClass = vm_find_class(env, \"java/lang/NullPointerException\");");
            w.println("                  if (npeClass) (*env)->ThrowNew(env, npeClass, \"\");");
            w.println("                  _hasException = 1; goto method_exit;");
            w.println("              }");
        }
        emitMethodHandleInvokeSpecialCase(w, isStatic, true);

        w.println("              #if VM_DEBUG_ENABLED");
        w.println("              const char* _cowner = vm_get_string(m->ownerIdx);");
        w.println("              const char* _cname  = vm_get_string(m->nameIdx);");
        w.println("              const char* _cdesc  = vm_get_string(m->descIdx);");
        w.println("              VM_LOG(\"INVOKE FROM %s.%s%s pc=%d -> %s.%s%s argc=%d\\n\", _cowner, _cname, _cdesc, invokePc, owner, name, desc, argCount);");
        w.println("              if (argTypes) VM_LOG(\"INVOKE ARGTYPES %s\\n\", argTypes);");
        if (!isStatic) {
            w.println("              if (owner && name && desc &&");
            w.println("                  strcmp(owner, \"java/lang/invoke/MethodHandle\") == 0 &&");
            w.println("                  strcmp(name, \"asCollector\") == 0 &&");
            w.println("                  strcmp(desc, \"(Ljava/lang/Class;I)Ljava/lang/invoke/MethodHandle;\") == 0) {");
            w.println("                  VM_LOG(\"asCollector recv=%p arg0=%p arg1=%d sp=%d\\n\", (void*)receiver, (void*)args[0].l, args[1].i, frame.sp);");
            w.println("              }");
        }
        w.println("              #endif");
        w.println("              VM_LOG_FLUSH();");

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
        w.println("                  const char* _owner = vm_get_string(m->ownerIdx);");
        w.println("                  const char* _name  = vm_get_string(m->nameIdx);");
        w.println("                  const char* _desc  = vm_get_string(m->descIdx);");
        w.println("                  VM_LOG(\"Exception thrown at pc=%d in %s.%s%s\\n\", invokePc, _owner, _name, _desc);");
        w.println("                  VM_LOG(\"Invoke target: %s.%s%s\\n\", owner, name, desc);");
        if (!isStatic) {
            w.println("                  if (owner && name && desc &&");
            w.println("                      strcmp(owner, \"java/lang/String\") == 0 &&");
            w.println("                      strcmp(name, \"substring\") == 0 &&");
            w.println("                      strcmp(desc, \"(II)Ljava/lang/String;\") == 0) {");
            w.println("                      VM_LOG(\"substring args: begin=%d, end=%d\\n\", args[0].i, args[1].i);");
            w.println("                      if (receiver) VM_LOG(\"substring recv len=%d\\n\", (*env)->GetStringLength(env, (jstring)receiver));");
            w.println("                  }");
        }
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

    private static void emitBuildJniArgsFromStack(PrintWriter w, String indent) {
        w.println(indent + "int _argc = argCount > 0 ? argCount : 1;");
        w.println(indent + "jvalue args[_argc];");
        w.println(indent + "switch (argCount) {");
        w.println(indent + "    case 0:");
        w.println(indent + "        break;");
        emitOneArgCase(w, indent, 1);
        emitOneArgCase(w, indent, 2);
        emitOneArgCase(w, indent, 3);
        emitOneArgCase(w, indent, 4);
        w.println(indent + "    default:");
        w.println(indent + "        for (int i = argCount - 1; i >= 0; i--) {");
        w.println(indent + "            char t = argTypes ? argTypes[i] : 'L';");
        emitArgExtractSwitch(w, indent + "            ", "t", "i");
        w.println(indent + "        }");
        w.println(indent + "        break;");
        w.println(indent + "}");
    }

    private static void emitOneArgCase(PrintWriter w, String indent, int count) {
        w.println(indent + "    case " + count + ": {");
        for (int idx = count - 1; idx >= 0; idx--) {
            String tVar = "_t" + idx;
            w.println(indent + "        char " + tVar + " = argTypes ? argTypes[" + idx + "] : 'L';");
            emitArgExtractSwitch(w, indent + "        ", tVar, Integer.toString(idx));
        }
        w.println(indent + "        break;");
        w.println(indent + "    }");
    }

    private static void emitArgExtractSwitch(PrintWriter w, String indent, String typeExpr, String argIndexExpr) {
        w.println(indent + "switch (" + typeExpr + ") {");
        w.println(indent + "    case 'I': case 'B': case 'C': case 'S': case 'Z':");
        w.println(indent + "        args[" + argIndexExpr + "].i = frame.stack[--frame.sp].i; break;");
        w.println(indent + "    case 'J': args[" + argIndexExpr + "].j = frame.stack[--frame.sp].j; break;");
        w.println(indent + "    case 'F': args[" + argIndexExpr + "].f = frame.stack[--frame.sp].f; break;");
        w.println(indent + "    case 'D': args[" + argIndexExpr + "].d = frame.stack[--frame.sp].d; break;");
        w.println(indent + "    default: args[" + argIndexExpr + "].l = frame.stack[--frame.sp].l; break;");
        w.println(indent + "}");
    }

    /**
     * Signature-polymorphic MethodHandle.invoke/invokeExact cannot be called through
     * reflective/JNI virtual dispatch directly. Route them via invokeWithArguments(Object[]).
     */
    private static void emitMethodHandleInvokeSpecialCase(PrintWriter w, boolean isStatic, boolean computedGoto) {
        if (isStatic) {
            return;
        }
        String indent = computedGoto ? "              " : "                  ";
        String dispatchOrContinue = computedGoto ? "DISPATCH_NEXT" : "continue";

        w.println(indent + "if (UNLIKELY(meta->flags & META_FLAG_MH_POLY_INVOKE)) {");
        w.println(indent + "    static jmethodID mhInvokeWithArgsMid = NULL;");
        w.println(indent + "    static jclass mhObjectClass = NULL;");
        w.println(indent + "    if (!mhInvokeWithArgsMid) {");
        w.println(indent + "        jclass mhCls = vm_find_class(env, \"java/lang/invoke/MethodHandle\");");
        w.println(indent + "        if (mhCls) mhInvokeWithArgsMid = vm_get_method_id(env, mhCls, \"java/lang/invoke/MethodHandle\", \"invokeWithArguments\", \"([Ljava/lang/Object;)Ljava/lang/Object;\");");
        w.println(indent + "    }");
        w.println(indent + "    if (!mhObjectClass) {");
        w.println(indent + "        mhObjectClass = vm_find_class(env, \"java/lang/Object\");");
        w.println(indent + "    }");
        w.println(indent + "    if (!mhInvokeWithArgsMid) {");
        w.println(indent + "        jclass le = vm_find_class(env, \"java/lang/LinkageError\");");
        w.println(indent + "        if (le) (*env)->ThrowNew(env, le, \"MethodHandle.invokeWithArguments not found\");");
        w.println(indent + "        _hasException = 1; goto method_exit;");
        w.println(indent + "    }");
        w.println(indent + "    jobjectArray mhArgs = (*env)->NewObjectArray(env, argCount, mhObjectClass, NULL);");
        w.println(indent + "    for (int mi = 0; mi < argCount && !(*env)->ExceptionCheck(env); mi++) {");
        w.println(indent + "        char t = argTypes ? argTypes[mi] : 'L';");
        w.println(indent + "        jobject boxed = NULL;");
        w.println(indent + "        switch (t) {");
        w.println(indent + "            case 'I': case 'B': case 'C': case 'S': case 'Z': { VMValue v; v.i = args[mi].i; boxed = vm_indy_box(env, t, v); break; }");
        w.println(indent + "            case 'J': { VMValue v; v.j = args[mi].j; boxed = vm_indy_box(env, t, v); break; }");
        w.println(indent + "            case 'F': { VMValue v; v.f = args[mi].f; boxed = vm_indy_box(env, t, v); break; }");
        w.println(indent + "            case 'D': { VMValue v; v.d = args[mi].d; boxed = vm_indy_box(env, t, v); break; }");
        w.println(indent + "            default: boxed = args[mi].l; break;");
        w.println(indent + "        }");
        w.println(indent + "        (*env)->SetObjectArrayElement(env, mhArgs, mi, boxed);");
        w.println(indent + "    }");
        w.println(indent + "    jobject mhResult = NULL;");
        w.println(indent + "    if (!(*env)->ExceptionCheck(env)) {");
        w.println(indent + "        mhResult = (*env)->CallObjectMethod(env, receiver, mhInvokeWithArgsMid, mhArgs);");
        w.println(indent + "    }");
        w.println(indent + "    if ((*env)->ExceptionCheck(env)) {");
        w.println(indent + "        jthrowable exc = (*env)->ExceptionOccurred(env);");
        w.println(indent + "        (*env)->ExceptionClear(env);");
        w.println(indent + "        int hPc = vm_find_exception_handler(env, m, invokePc, exc);");
        w.println(indent + "        if (hPc >= 0) {");
        w.println(indent + "            frame.sp = 0;");
        w.println(indent + "            frame.stack[frame.sp++].l = exc;");
        w.println(indent + "            frame.pc = hPc;");
        w.println(indent + "            " + dispatchOrContinue + ";");
        w.println(indent + "        }");
        w.println(indent + "        (*env)->Throw(env, exc);");
        w.println(indent + "        _hasException = 1; goto method_exit;");
        w.println(indent + "    }");
        w.println(indent + "    if (!vm_indy_push_return(env, &frame, meta, mhResult)) {");
        w.println(indent + "        if ((*env)->ExceptionCheck(env)) {");
        w.println(indent + "            jthrowable exc = (*env)->ExceptionOccurred(env);");
        w.println(indent + "            (*env)->ExceptionClear(env);");
        w.println(indent + "            int hPc = vm_find_exception_handler(env, m, invokePc, exc);");
        w.println(indent + "            if (hPc >= 0) {");
        w.println(indent + "                frame.sp = 0;");
        w.println(indent + "                frame.stack[frame.sp++].l = exc;");
        w.println(indent + "                frame.pc = hPc;");
        w.println(indent + "                " + dispatchOrContinue + ";");
        w.println(indent + "            }");
        w.println(indent + "            (*env)->Throw(env, exc);");
        w.println(indent + "        }");
        w.println(indent + "        _hasException = 1; goto method_exit;");
        w.println(indent + "    }");
        w.println(indent + "    frame.pc++;");
        w.println(indent + "    " + dispatchOrContinue + ";");
        w.println(indent + "}");
    }

    /**
     * Emit the direct VM-to-VM call body (shared by switch and computed goto versions)
     * Pops args from caller stack, builds callee locals, calls vm_execute_common directly.
     */
    private static void emitDirectCallBody(PrintWriter w, boolean isStatic, boolean computedGoto) {
        String indent = computedGoto ? "                  " : "                      ";
        String dispatchOrContinue = computedGoto ? "DISPATCH_NEXT" : "continue";

        // Build callee locals directly from caller's stack
        w.println(indent + "VM_PERF_DIRECT_HIT();");
        w.println(indent + "int targetMaxLocals = vm_methods[vmTargetId].maxLocals;");
        w.println(indent + "int localCap = targetMaxLocals > 0 ? targetMaxLocals : 1;");
        w.println(indent + "VMValue tempLocals[localCap];");

        // Pop args from caller stack (reverse order) into callee locals.
        // Fast path uses precomputed pop-order template (argPopMap).
        w.println(indent + "if (LIKELY(meta->argPopMap != NULL)) {");
        w.println(indent + "    int argBase = " + (isStatic ? "0" : "1") + ";");
        w.println(indent + "    int* popMap = meta->argPopMap;");
        w.println(indent + "    switch (argCount) {");
        w.println(indent + "        case 0:");
        w.println(indent + "            break;");
        w.println(indent + "        case 1:");
        w.println(indent + "            tempLocals[popMap[0] + argBase] = frame.stack[--frame.sp];");
        w.println(indent + "            break;");
        w.println(indent + "        case 2:");
        w.println(indent + "            tempLocals[popMap[0] + argBase] = frame.stack[--frame.sp];");
        w.println(indent + "            tempLocals[popMap[1] + argBase] = frame.stack[--frame.sp];");
        w.println(indent + "            break;");
        w.println(indent + "        case 3:");
        w.println(indent + "            tempLocals[popMap[0] + argBase] = frame.stack[--frame.sp];");
        w.println(indent + "            tempLocals[popMap[1] + argBase] = frame.stack[--frame.sp];");
        w.println(indent + "            tempLocals[popMap[2] + argBase] = frame.stack[--frame.sp];");
        w.println(indent + "            break;");
        w.println(indent + "        case 4:");
        w.println(indent + "            tempLocals[popMap[0] + argBase] = frame.stack[--frame.sp];");
        w.println(indent + "            tempLocals[popMap[1] + argBase] = frame.stack[--frame.sp];");
        w.println(indent + "            tempLocals[popMap[2] + argBase] = frame.stack[--frame.sp];");
        w.println(indent + "            tempLocals[popMap[3] + argBase] = frame.stack[--frame.sp];");
        w.println(indent + "            break;");
        w.println(indent + "        default:");
        w.println(indent + "            for (int pi = 0; pi < argCount; pi++) {");
        w.println(indent + "                tempLocals[popMap[pi] + argBase] = frame.stack[--frame.sp];");
        w.println(indent + "            }");
        w.println(indent + "            break;");
        w.println(indent + "    }");
        w.println(indent + "} else {");
        if (isStatic) {
            w.println(indent + "    int localCursor = meta->argLocalSlots;");
        } else {
            w.println(indent + "    int localCursor = 1 + meta->argLocalSlots;");
        }
        w.println(indent + "    for (int i = argCount - 1; i >= 0; i--) {");
        w.println(indent + "        int _wide = (i < 64) ? (int)((meta->argWideMask >> i) & 1ULL) : 0;");
        w.println(indent + "        if (UNLIKELY(i >= 64 && argTypes != NULL)) {");
        w.println(indent + "            char _t = argTypes[i];");
        w.println(indent + "            _wide = (_t == 'J' || _t == 'D') ? 1 : 0;");
        w.println(indent + "        }");
        w.println(indent + "        localCursor -= _wide ? 2 : 1;");
        w.println(indent + "        tempLocals[localCursor] = frame.stack[--frame.sp];");
        w.println(indent + "    }");
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

        // Call vm_execute_common directly with pre-built locals.
        // callerClass must match callee owner class (same as bridge path), otherwise
        // caller-sensitive logic (e.g., invokedynamic Lookup context) may break.
        w.println(indent + "jclass directCallerClass = frame.callerClass;");
        w.println(indent + "if (owner) {");
        w.println(indent + "    jclass _calleeOwnerClass = meta->directOwnerClass;");
        w.println(indent + "    if (_calleeOwnerClass == NULL) {");
        w.println(indent + "        _calleeOwnerClass = vm_find_class(env, owner);");
        w.println(indent + "        if (_calleeOwnerClass != NULL) meta->directOwnerClass = _calleeOwnerClass;");
        w.println(indent + "    }");
        w.println(indent + "    if (_calleeOwnerClass) directCallerClass = _calleeOwnerClass;");
        w.println(indent + "}");
        w.println(indent + "int obfTargetId = vmTargetId ^ METHOD_ID_XOR_KEY;");
        w.println(indent + "ExecuteResult directResult = vm_execute_common(env, obfTargetId, NULL, tempLocals, localCap, directCallerClass);");

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
