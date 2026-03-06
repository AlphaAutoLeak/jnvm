package com.alphaautoleak.jnvm.codegen.emitter.instruction.control;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * ATHROW instruction - throw exception
 * 
 * 注意：ATHROW 需要在异常表中查找 handler，而不是直接返回。
 * 如果找到 handler，跳转到 handler；否则重新抛出异常让外层处理。
 */
public class AThrowInstruction extends Instruction {
    public AThrowInstruction() {
        super(0xbf, "ATHROW");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        // 添加空语句，避免 C23 警告 (label 后不能直接跟声明)
        w.println("                ;");
        // Pop exception object from stack
        w.println("                jobject exception = frame.stack[--frame.sp].l;");
        w.println("                if (exception == NULL) {");
        w.println("                    // Throw NullPointerException for null exception");
        w.println("                    jclass npeClass = vm_find_class(env, \"java/lang/NullPointerException\");");
        w.println("                    if (npeClass) (*env)->ThrowNew(env, npeClass, \"Cannot throw null exception\");");
        w.println("                    goto method_exit;");
        w.println("                }");
        w.println("                VM_LOG(\"ATHROW: exception thrown at pc=%d\\n\", frame.pc);");
        w.println("                ");
        w.println("                // 在异常表中查找匹配的 handler");
        w.println("                int athrowHandlerPc = -1;");
        w.println("                if (m->exceptionTable != NULL && m->exceptionTableLength > 0) {");
        w.println("                    for (int i = 0; i < m->exceptionTableLength; i++) {");
        w.println("                        VMExceptionEntry* entry = &m->exceptionTable[i];");
        w.println("                        if (frame.pc >= entry->startPc && frame.pc < entry->endPc) {");
        w.println("                            // catchTypeIdx == -1 表示 catch-all (finally)");
        w.println("                            if (entry->catchTypeIdx < 0) {");
        w.println("                                athrowHandlerPc = entry->handlerPc;");
        w.println("                                VM_LOG(\"ATHROW: Found catch-all handler at pc=%d\\n\", athrowHandlerPc);");
        w.println("                                break;");
        w.println("                            }");
        w.println("                            // 检查异常类型是否匹配");
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
        w.println("                    // 找到 handler，将异常对象压入栈并跳转");
        w.println("                    frame.sp = 0;  // 清空栈");
        w.println("                    frame.stack[frame.sp++].l = exception;");
        w.println("                    frame.pc = athrowHandlerPc;");
        w.println("                    VM_LOG(\"ATHROW: Jumping to handler at pc=%d\\n\", athrowHandlerPc);");
        w.println("                } else {");
        w.println("                    // 没有找到 handler，重新抛出异常");
        w.println("                    VM_LOG(\"ATHROW: No handler found, rethrowing\\n\");");
        w.println("                    (*env)->Throw(env, (jthrowable)exception);");
        w.println("                    goto method_exit;");
        w.println("                }");
    }
    
    @Override
    public void generate(PrintWriter w) {
        w.printf("            case 0x%02x: /* %s */\n", opcode, comment);
        generateBody(w);
        w.println("                break;");
    }
}