package com.alphaautoleak.jnvm.codegen.emitter.instruction.arithmetic;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * 比较指令 (LCMP, FCMPL, FCMPG, DCMPL, DCMPG)
 */
public class CmpInstruction extends Instruction {
    private final String type;
    private final String jniType;

    public CmpInstruction(int opcode, String name, String type) {
        super(opcode, name);
        this.type = type;
        this.jniType = getJniType(type);
    }

    private static String getJniType(String type) {
        if (type.equals("i")) return "jint";
        if (type.equals("j")) return "jlong";
        if (type.equals("f")) return "jfloat";
        if (type.equals("d")) return "jdouble";
        return type;
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { " + jniType + " b = frame.stack[--frame.sp]." + type + ";");
        w.println("                  " + jniType + " a = frame.stack[--frame.sp]." + type + ";");
        w.println("                  frame.stack[frame.sp++].i = (a < b) ? -1 : ((a > b) ? 1 : 0); }");
        pcIncBreak(w);
    }
}
