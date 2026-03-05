package com.alphaautoleak.jnvm.codegen.emitter.instruction.arithmetic;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * Division instruction (IDIV, LDIV, IREM, LREM)
 * Also handles FREM, DREM using fmod functions
 */
public class DivInstruction extends Instruction {
    private final String type;
    private final boolean isRem;
    private final String vmType;
    private final boolean isFloat;

    public DivInstruction(int opcode, String name, String type) {
        this(opcode, name, type, false);
    }

    public DivInstruction(int opcode, String name, String type, boolean isRem) {
        super(opcode, name);
        this.type = type;
        this.isRem = isRem;
        this.vmType = getVmType(type);
        this.isFloat = type.equals("f") || type.equals("d");
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
        if (isFloat && isRem) {
            // Float/Double remainder: use fmod/fmodl
            String fmodFunc = type.equals("f") ? "fmodf" : "fmod";
            w.println("                {");
            w.println("                    j" + (type.equals("f") ? "float" : "double") + " v2 = frame.stack[frame.sp-1]." + type + ";");
            w.println("                    j" + (type.equals("f") ? "float" : "double") + " v1 = frame.stack[frame.sp-2]." + type + ";");
            w.println("                    frame.stack[frame.sp-2]." + type + " = " + fmodFunc + "(v1, v2);");
            w.println("                    frame.stackTypes[frame.sp-2] = " + vmType + ";");
            w.println("                    frame.sp--;");
            w.println("                }");
        } else if (isRem) {
            // Integer/Long remainder
            w.println("                if (frame.stack[frame.sp-1]." + type + " != 0) {");
            w.println("                    frame.stack[frame.sp-2]." + type + " %= frame.stack[frame.sp-1]." + type + ";");
            w.println("                    frame.stackTypes[frame.sp-2] = " + vmType + ";");
            w.println("                }");
            w.println("                frame.sp--;");
        } else {
            // Division
            w.println("                if (frame.stack[frame.sp-1]." + type + " != 0) {");
            w.println("                    frame.stack[frame.sp-2]." + type + " /= frame.stack[frame.sp-1]." + type + ";");
            w.println("                    frame.stackTypes[frame.sp-2] = " + vmType + ";");
            w.println("                }");
            w.println("                frame.sp--;");
        }
        pcIncBreak(w);
    }
}
