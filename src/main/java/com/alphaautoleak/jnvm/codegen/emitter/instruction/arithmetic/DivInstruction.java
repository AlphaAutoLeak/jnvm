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
        } else if (isRem) {
            w.println("                if (frame.stack[frame.sp-1]." + type + " != 0) {");
            w.println("                    frame.stack[frame.sp-2]." + type + " %= frame.stack[frame.sp-1]." + type + ";");
            w.println("                }");
            w.println("                frame.sp--;");
        } else {
            w.println("                if (frame.stack[frame.sp-1]." + type + " != 0) {");
            w.println("                    frame.stack[frame.sp-2]." + type + " /= frame.stack[frame.sp-1]." + type + ";");
            w.println("                }");
            w.println("                frame.sp--;");
        }
        pcIncBreak(w);
    }
}