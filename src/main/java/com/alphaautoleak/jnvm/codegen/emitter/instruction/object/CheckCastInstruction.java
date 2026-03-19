package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * CHECKCAST instruction
 */
public class CheckCastInstruction extends Instruction {
    public CheckCastInstruction() {
        super(0xc0, "CHECKCAST");
    }
    
    @Override
    public boolean needsMeta() {
        return true;
    }

    @Override
    protected void generateBody(PrintWriter w) {
        // Unused because CHECKCAST needs dedicated switch/computed-goto exception routing.
        w.println("                frame.pc++;");
    }

    @Override
    public void generate(PrintWriter w) {
        w.printf("            case 0x%02x: /* %s */\n", opcode, comment);
        w.println("                {");
        w.println("                    if (!meta) { frame.pc++; break; }");
        w.println("                    jobject obj = frame.stack[frame.sp - 1].l;");
        w.println("                    if (obj) {");
        w.println("                        const char* clsName = meta->classStr ? meta->classStr : vm_get_string(meta->classIdx);");
        w.println("                        jclass cls = vm_find_class(env, clsName);");
        w.println("                        if (!cls) {");
        emitSwitchExceptionRouting(w, "                            ");
        w.println("                        }");
        w.println("                        if (!(*env)->IsInstanceOf(env, obj, cls)) {");
        w.println("                            jclass cceClass = vm_find_class(env, \"java/lang/ClassCastException\");");
        w.println("                            if (cceClass) {");
        w.println("                                (*env)->ThrowNew(env, cceClass, clsName ? clsName : \"CHECKCAST failed\");");
        w.println("                            }");
        emitSwitchExceptionRouting(w, "                            ");
        w.println("                        }");
        w.println("                    }");
        w.println("                }");
        w.println("                frame.pc++;");
        w.println("                break;");
    }

    @Override
    public void generateComputedGoto(PrintWriter w) {
        w.printf("        OP_%02x:  /* %s */\n", opcode, comment);
        w.println("            {");
        w.println("                if (!meta) { frame.pc++; DISPATCH_NEXT; }");
        w.println("                jobject obj = frame.stack[frame.sp - 1].l;");
        w.println("                if (obj) {");
        w.println("                    const char* clsName = meta->classStr ? meta->classStr : vm_get_string(meta->classIdx);");
        w.println("                    jclass cls = vm_find_class(env, clsName);");
        w.println("                    if (!cls) {");
        emitComputedGotoExceptionRouting(w, "                        ");
        w.println("                    }");
        w.println("                    if (!(*env)->IsInstanceOf(env, obj, cls)) {");
        w.println("                        jclass cceClass = vm_find_class(env, \"java/lang/ClassCastException\");");
        w.println("                        if (cceClass) {");
        w.println("                            (*env)->ThrowNew(env, cceClass, clsName ? clsName : \"CHECKCAST failed\");");
        w.println("                        }");
        emitComputedGotoExceptionRouting(w, "                        ");
        w.println("                    }");
        w.println("                }");
        w.println("            }");
        w.println("            frame.pc++;");
        w.println("            DISPATCH_NEXT;");
    }

    private void emitSwitchExceptionRouting(PrintWriter w, String indent) {
        w.println(indent + "if ((*env)->ExceptionCheck(env)) {");
        w.println(indent + "    jthrowable exc = (*env)->ExceptionOccurred(env);");
        w.println(indent + "    (*env)->ExceptionClear(env);");
        w.println(indent + "    int hPc = vm_find_exception_handler(env, m, frame.pc, exc);");
        w.println(indent + "    if (hPc >= 0) {");
        w.println(indent + "        frame.sp = 0;");
        w.println(indent + "        frame.stack[frame.sp++].l = exc;");
        w.println(indent + "        frame.pc = hPc;");
        w.println(indent + "        continue;");
        w.println(indent + "    }");
        w.println(indent + "    (*env)->Throw(env, exc);");
        w.println(indent + "}");
        w.println(indent + "_hasException = 1; goto method_exit;");
    }

    private void emitComputedGotoExceptionRouting(PrintWriter w, String indent) {
        w.println(indent + "if ((*env)->ExceptionCheck(env)) {");
        w.println(indent + "    jthrowable exc = (*env)->ExceptionOccurred(env);");
        w.println(indent + "    (*env)->ExceptionClear(env);");
        w.println(indent + "    int hPc = vm_find_exception_handler(env, m, frame.pc, exc);");
        w.println(indent + "    if (hPc >= 0) {");
        w.println(indent + "        frame.sp = 0;");
        w.println(indent + "        frame.stack[frame.sp++].l = exc;");
        w.println(indent + "        frame.pc = hPc;");
        w.println(indent + "        DISPATCH_NEXT;");
        w.println(indent + "    }");
        w.println(indent + "    (*env)->Throw(env, exc);");
        w.println(indent + "}");
        w.println(indent + "_hasException = 1; goto method_exit;");
    }
}
