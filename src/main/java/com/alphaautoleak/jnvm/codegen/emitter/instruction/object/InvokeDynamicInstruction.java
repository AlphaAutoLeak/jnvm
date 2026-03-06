package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * INVOKEDYNAMIC instruction
 */
public class InvokeDynamicInstruction extends Instruction {
    public InvokeDynamicInstruction() {
        super(0xba, "INVOKEDYNAMIC");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { int invokePc = frame.pc;  // Save PC before invocation");
        w.println("                  // INVOKEDYNAMIC - Lambda support");
        w.println("                  jobject result = vm_invoke_dynamic(env, &frame, meta);");
        w.println("                  if (result != NULL) frame.stack[frame.sp++].l = result;");
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
        w.println("                frame.pc++;");  // 在保护的字节码格式中，每条指令只有 1 字节
    }

    @Override
    public void generateComputedGoto(PrintWriter w) {
        w.printf("        OP_%02x:  /* %s */\n", opcode, comment);
        w.println("            { int invokePc = frame.pc;");
        w.println("              jobject result = vm_invoke_dynamic(env, &frame, meta);");
        w.println("              if (result != NULL) frame.stack[frame.sp++].l = result;");
        w.println("              if ((*env)->ExceptionCheck(env)) {");
        w.println("                  VM_LOG(\"Exception thrown at pc=%d\\n\", invokePc);");
        w.println("                  jthrowable exc = (*env)->ExceptionOccurred(env);");
        w.println("                  (*env)->ExceptionClear(env);");
        w.println("                  int hPc = vm_find_exception_handler(env, m, invokePc, exc);");
        w.println("                  if (hPc >= 0) {");
        w.println("                      frame.sp = 0;");
        w.println("                      frame.stack[frame.sp++].l = exc;");
        w.println("                      frame.pc = hPc;");
        w.println("                      DISPATCH_NEXT;");  // 使用 DISPATCH_NEXT
        w.println("                  }");
        w.println("                  VM_LOG(\"No handler found, rethrowing\\n\");");
        w.println("                  (*env)->Throw(env, exc);");
        w.println("                  goto method_exit;");
        w.println("              }");
        w.println("            }");
        w.println("            frame.pc++;");
        w.println("            DISPATCH_NEXT;");
    }
}