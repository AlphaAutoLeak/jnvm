package com.alphaautoleak.jnvm.codegen.emitter.instruction.arithmetic;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * Division instruction (IDIV, LDIV, IREM, LREM, FREM, DREM) - 64-bit only
 */
public class DivInstruction extends Instruction {
    private final String type;
    private final boolean isRem;
    private final boolean isFloat;

    public DivInstruction(int opcode, String name, String type) {
        this(opcode, name, type, false);
    }

    public DivInstruction(int opcode, String name, String type, boolean isRem) {
        super(opcode, name);
        this.type = type;
        this.isRem = isRem;
        this.isFloat = type.equals("f") || type.equals("d");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        if (isFloat && isRem) {
            String fmodFunc = type.equals("f") ? "fmodf" : "fmod";
            w.println("                {");
            w.println("                    j" + (type.equals("f") ? "float" : "double") + " v2 = frame.stack[frame.sp-1]." + type + ";");
            w.println("                    j" + (type.equals("f") ? "float" : "double") + " v1 = frame.stack[frame.sp-2]." + type + ";");
            w.println("                    frame.stack[frame.sp-2]." + type + " = " + fmodFunc + "(v1, v2);");
            w.println("                    frame.sp--;");
            w.println("                }");
        } else if (isFloat) {
            w.println("                frame.stack[frame.sp-2]." + type + " /= frame.stack[frame.sp-1]." + type + ";");
            w.println("                frame.sp--;");
        } else if (isRem) {
            if ("i".equals(type)) {
                w.println("                {");
                w.println("                    jint v2 = frame.stack[frame.sp-1].i;");
                w.println("                    jint v1 = frame.stack[frame.sp-2].i;");
                w.println("                    if (UNLIKELY(v2 == 0)) {");
                w.println("                        jclass ae = vm_find_class(env, \"java/lang/ArithmeticException\");");
                w.println("                        if (ae) (*env)->ThrowNew(env, ae, \"/ by zero\");");
                w.println("                        _hasException = 1; goto method_exit;");
                w.println("                    }");
                w.println("                    if (v1 == (jint)0x80000000 && v2 == -1) frame.stack[frame.sp-2].i = 0;");
                w.println("                    else frame.stack[frame.sp-2].i = v1 % v2;");
                w.println("                    frame.sp--;");
                w.println("                }");
            } else {
                w.println("                {");
                w.println("                    jlong v2 = frame.stack[frame.sp-1].j;");
                w.println("                    jlong v1 = frame.stack[frame.sp-2].j;");
                w.println("                    if (UNLIKELY(v2 == 0)) {");
                w.println("                        jclass ae = vm_find_class(env, \"java/lang/ArithmeticException\");");
                w.println("                        if (ae) (*env)->ThrowNew(env, ae, \"/ by zero\");");
                w.println("                        _hasException = 1; goto method_exit;");
                w.println("                    }");
                w.println("                    if (v1 == (jlong)0x8000000000000000LL && v2 == -1LL) frame.stack[frame.sp-2].j = 0LL;");
                w.println("                    else frame.stack[frame.sp-2].j = v1 % v2;");
                w.println("                    frame.sp--;");
                w.println("                }");
            }
        } else {
            if ("i".equals(type)) {
                w.println("                {");
                w.println("                    jint v2 = frame.stack[frame.sp-1].i;");
                w.println("                    jint v1 = frame.stack[frame.sp-2].i;");
                w.println("                    if (UNLIKELY(v2 == 0)) {");
                w.println("                        jclass ae = vm_find_class(env, \"java/lang/ArithmeticException\");");
                w.println("                        if (ae) (*env)->ThrowNew(env, ae, \"/ by zero\");");
                w.println("                        _hasException = 1; goto method_exit;");
                w.println("                    }");
                w.println("                    if (v1 == (jint)0x80000000 && v2 == -1) frame.stack[frame.sp-2].i = (jint)0x80000000;");
                w.println("                    else frame.stack[frame.sp-2].i = v1 / v2;");
                w.println("                    frame.sp--;");
                w.println("                }");
            } else {
                w.println("                {");
                w.println("                    jlong v2 = frame.stack[frame.sp-1].j;");
                w.println("                    jlong v1 = frame.stack[frame.sp-2].j;");
                w.println("                    if (UNLIKELY(v2 == 0)) {");
                w.println("                        jclass ae = vm_find_class(env, \"java/lang/ArithmeticException\");");
                w.println("                        if (ae) (*env)->ThrowNew(env, ae, \"/ by zero\");");
                w.println("                        _hasException = 1; goto method_exit;");
                w.println("                    }");
                w.println("                    if (v1 == (jlong)0x8000000000000000LL && v2 == -1LL) frame.stack[frame.sp-2].j = (jlong)0x8000000000000000LL;");
                w.println("                    else frame.stack[frame.sp-2].j = v1 / v2;");
                w.println("                    frame.sp--;");
                w.println("                }");
            }
        }
        pcIncBreak(w);
    }
}
