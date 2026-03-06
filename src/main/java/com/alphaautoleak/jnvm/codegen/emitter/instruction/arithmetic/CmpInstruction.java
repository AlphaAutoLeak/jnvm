package com.alphaautoleak.jnvm.codegen.emitter.instruction.arithmetic;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * Compare instruction (LCMP, FCMPL, FCMPG, DCMPL, DCMPG) - 64-bit only
 */
public class CmpInstruction extends Instruction {
    private final String type;
    private final String jniType;
    private final boolean isFloatOrDouble;
    private final boolean isCmpL;

    public CmpInstruction(int opcode, String name, String type) {
        super(opcode, name);
        this.type = type;
        this.jniType = getJniType(type);
        this.isFloatOrDouble = type.equals("f") || type.equals("d");
        this.isCmpL = name.endsWith("L");
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
        if (isFloatOrDouble) {
            String nanResult = isCmpL ? "-1" : "1";
            w.println("                  int cmpResult;");
            w.println("                  if (a != a || b != b) { cmpResult = " + nanResult + "; }");
            w.println("                  else if (a < b) { cmpResult = -1; }");
            w.println("                  else if (a > b) { cmpResult = 1; }");
            w.println("                  else { cmpResult = 0; }");
            w.println("                  frame.stack[frame.sp++].i = cmpResult; }");
        } else {
            w.println("                  frame.stack[frame.sp++].i = (a < b) ? -1 : ((a > b) ? 1 : 0); }");
        }
        pcIncBreak(w);
    }
}
