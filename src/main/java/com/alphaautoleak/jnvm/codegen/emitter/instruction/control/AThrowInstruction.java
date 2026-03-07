package com.alphaautoleak.jnvm.codegen.emitter.instruction.control;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * ATHROW instruction - throw exception
 * 
 * Note: ATHROW needs to find handler in exception table instead of returning directly.
 * If handler found, jump to it; otherwise rethrow for outer handler.
 */
public class AThrowInstruction extends Instruction {
    public AThrowInstruction() {
        super(0xbf, "ATHROW");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        // Empty statement to avoid C23 warning (declaration after label)
        w.println("                ;");
        // Pop exception object from stack
        w.println("                jobject exception = frame.stack[--frame.sp].l;");
        w.println("                if (exception == NULL) {");
        w.println("                    // Throw NullPointerException for null exception");
        w.println("                    jclass npeClass = vm_find_class(env, \"java/lang/NullPointerException\");");
        w.println("                    if (npeClass) (*env)->ThrowNew(env, npeClass, \"Cannot throw null exception\");");
        w.println("                    _hasException = 1; goto method_exit;");
        w.println("                }");
        w.println("                VM_LOG(\"ATHROW: exception thrown at pc=%d\\n\", frame.pc);");
        w.println("                ");
        w.println("                // Find matching handler in exception table");
        w.println("                int athrowHandlerPc = -1;");
        w.println("                if (m->exceptionTable != NULL && m->exceptionTableLength > 0) {");
        w.println("                    for (int i = 0; i < m->exceptionTableLength; i++) {");
        w.println("                        VMExceptionEntry* entry = &m->exceptionTable[i];");
        w.println("                        if (frame.pc >= entry->startPc && frame.pc < entry->endPc) {");
        w.println("                            // catchTypeIdx == -1 means catch-all (finally)");
        w.println("                            if (entry->catchTypeIdx < 0) {");
        w.println("                                athrowHandlerPc = entry->handlerPc;");
        w.println("                                VM_LOG(\"ATHROW: Found catch-all handler at pc=%d\\n\", athrowHandlerPc);");
        w.println("                                break;");
        w.println("                            }");
        w.println("                            // Check if exception type matches");
        w.println("                            const char* catchType = vm_get_string(entry->catchTypeIdx);");
        w.println("                            jclass catchClass = vm_find_class(env, catchType);");
        w.println("                            if (catchClass && (*env)->IsInstanceOf(env, exception, catchClass)) {");
        w.println("                                athrowHandlerPc = entry->handlerPc;");
        w.println("                                VM_LOG(\"ATHROW: Found handler for %s at pc=%d\\n\", catchType, athrowHandlerPc);");
        w.println("                                break;");
        w.println("                            }");
        w.println("                        }");
        w.println("                    }");
        w.println("                }");
        w.println("                ");
        w.println("                if (athrowHandlerPc >= 0) {");
        w.println("                    // Found handler, push exception and jump");
        w.println("                    frame.sp = 0;  // clear stack");
        w.println("                    frame.stack[frame.sp++].l = exception;");
        w.println("                    frame.pc = athrowHandlerPc;");
        w.println("                    VM_LOG(\"ATHROW: Jumping to handler at pc=%d\\n\", athrowHandlerPc);");
        w.println("                } else {");
        w.println("                    // No handler found, rethrow exception");
        w.println("                    VM_LOG(\"ATHROW: No handler found, rethrowing\\n\");");
        w.println("                    (*env)->Throw(env, (jthrowable)exception);");
        w.println("                    _hasException = 1; goto method_exit;");
        w.println("                }");
    }
    
    @Override
    protected boolean needsPcIncrement() {
        return false;
    }

    @Override
    public void generate(PrintWriter w) {
        w.printf("            case 0x%02x: /* %s */\n", opcode, comment);
        generateBody(w);
        w.println("                break;");
    }

    @Override
    public void generateComputedGoto(PrintWriter w) {
        w.printf("        OP_%02x:  /* %s */\n", opcode, comment);
        w.println("            ;");  // avoid declaration directly after label
        // Pop exception object from stack
        w.println("            jobject exception = frame.stack[--frame.sp].l;");
        w.println("            if (exception == NULL) {");
        w.println("                jclass npeClass = vm_find_class(env, \"java/lang/NullPointerException\");");
        w.println("                if (npeClass) (*env)->ThrowNew(env, npeClass, \"Cannot throw null exception\");");
        w.println("                _hasException = 1; goto method_exit;");
        w.println("            }");
        w.println("            VM_LOG(\"ATHROW: exception thrown at pc=%d\\n\", frame.pc);");
        w.println("            ");
        w.println("            // Find matching handler in exception table");
        w.println("            int athrowHandlerPc = -1;");
        w.println("            if (m->exceptionTable != NULL && m->exceptionTableLength > 0) {");
        w.println("                for (int i = 0; i < m->exceptionTableLength; i++) {");
        w.println("                    VMExceptionEntry* entry = &m->exceptionTable[i];");
        w.println("                    if (frame.pc >= entry->startPc && frame.pc < entry->endPc) {");
        w.println("                        if (entry->catchTypeIdx < 0) {");
        w.println("                            athrowHandlerPc = entry->handlerPc;");
        w.println("                            VM_LOG(\"ATHROW: Found catch-all handler at pc=%d\\n\", athrowHandlerPc);");
        w.println("                            break;");
        w.println("                        }");
        w.println("                        const char* catchType = vm_get_string(entry->catchTypeIdx);");
        w.println("                        jclass catchClass = vm_find_class(env, catchType);");
        w.println("                        if (catchClass && (*env)->IsInstanceOf(env, exception, catchClass)) {");
        w.println("                            athrowHandlerPc = entry->handlerPc;");
        w.println("                            VM_LOG(\"ATHROW: Found handler for %s at pc=%d\\n\", catchType, athrowHandlerPc);");
        w.println("                            break;");
        w.println("                        }");
        w.println("                    }");
        w.println("                }");
        w.println("            }");
        w.println("            ");
        w.println("            if (athrowHandlerPc >= 0) {");
        w.println("                frame.sp = 0;");
        w.println("                frame.stack[frame.sp++].l = exception;");
        w.println("                frame.pc = athrowHandlerPc;");
        w.println("                VM_LOG(\"ATHROW: Jumping to handler at pc=%d\\n\", athrowHandlerPc);");
        w.println("                DISPATCH_NEXT;");  // found handler, continue execution
        w.println("            } else {");
        w.println("                VM_LOG(\"ATHROW: No handler found, rethrowing\\n\");");
        w.println("                (*env)->Throw(env, (jthrowable)exception);");
        w.println("                _hasException = 1; goto method_exit;");  // no handler found, exit
        w.println("            }");
    }
}