package com.alphaautoleak.jnvm.codegen.emitter.instruction.arithmetic;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * Division instruction (IDIV, LDIV, IREM, LREM)
 */
public class DivInstruction extends Instruction {
    private final String type;
    private final boolean isRem;
    private final String vmType;

    public DivInstruction(int opcode, String name, String type) {
        this(opcode, name, type, false);
    }

    public DivInstruction(int opcode, String name, String type, boolean isRem) {
        super(opcode, name);
        this.type = type;
        this.isRem = isRem;
        this.vmType = getVmType(type);
    }

    private static String getVmType(String type) {
        switch (type) {
            case "i": return "TYPE_INT";
            case "j": return "TYPE_LONG";
            case "f": return "TYPE_FLOAT";
            case "d": return "TYPE_DOUBLE";
            default: return "TYPE_UNKNOWN";
        }
    }

    @Override
    protected void generateBody(PrintWriter w) {
        String op = isRem ? "%" : "/";
        w.println("                if (frame.stack[frame.sp-1]." + type + " != 0) {");
        w.println("                    frame.stack[frame.sp-2]." + type + " " + op + "= frame.stack[frame.sp-1]." + type + ";");
        w.println("                    frame.stackTypes[frame.sp-2] = " + vmType + ";");
        w.println("                }");
        w.println("                frame.sp--;");
        pcIncBreak(w);
    }
}
