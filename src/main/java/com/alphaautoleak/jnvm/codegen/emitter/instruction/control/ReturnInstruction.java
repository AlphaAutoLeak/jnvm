package com.alphaautoleak.jnvm.codegen.emitter.instruction.control;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * Return instruction (RETURN, IRETURN, LRETURN, FRETURN, DRETURN, ARETURN)
 * 
 * 重要：由于 VMBridge.execute() 返回 Object，native 端必须将基本类型返回值装箱。
 * Java 端的 JarPatcher 会根据方法返回类型进行拆箱。
 */
public class ReturnInstruction extends Instruction {
    private final String returnType; // "void", "int", "long", "float", "double", "object"

    public ReturnInstruction(int opcode, String name, String returnType) {
        super(opcode, name);
        this.returnType = returnType;
    }

    @Override
    protected void generateBody(PrintWriter w) {
        switch (returnType) {
            case "void":
                w.println("                return NULL;");
                break;
            case "int":
                // int 需要装箱为 Integer
                w.println("                { jint val = frame.stack[--frame.sp].i;");
                w.println("                  jclass cls = (*env)->FindClass(env, \"java/lang/Integer\");");
                w.println("                  jmethodID mid = (*env)->GetMethodID(env, cls, \"<init>\", \"(I)V\");");
                w.println("                  return (*env)->NewObject(env, cls, mid, val); }");
                break;
            case "long":
                // long 需要装箱为 Long
                w.println("                { jlong val = frame.stack[--frame.sp].j;");
                w.println("                  jclass cls = (*env)->FindClass(env, \"java/lang/Long\");");
                w.println("                  jmethodID mid = (*env)->GetMethodID(env, cls, \"<init>\", \"(J)V\");");
                w.println("                  return (*env)->NewObject(env, cls, mid, val); }");
                break;
            case "float":
                // float 需要装箱为 Float
                w.println("                { jfloat val = frame.stack[--frame.sp].f;");
                w.println("                  jclass cls = (*env)->FindClass(env, \"java/lang/Float\");");
                w.println("                  jmethodID mid = (*env)->GetMethodID(env, cls, \"<init>\", \"(F)V\");");
                w.println("                  return (*env)->NewObject(env, cls, mid, val); }");
                break;
            case "double":
                // double 需要装箱为 Double
                w.println("                { jdouble val = frame.stack[--frame.sp].d;");
                w.println("                  jclass cls = (*env)->FindClass(env, \"java/lang/Double\");");
                w.println("                  jmethodID mid = (*env)->GetMethodID(env, cls, \"<init>\", \"(D)V\");");
                w.println("                  return (*env)->NewObject(env, cls, mid, val); }");
                break;
            case "object":
            default:
                // 对象类型直接返回
                w.println("                return frame.stack[--frame.sp].l;");
                break;
        }
    }

    @Override
    public void generate(PrintWriter w) {
        w.printf("            case 0x%02x: /* %s */\n", opcode, comment);
        generateBody(w);
    }
}