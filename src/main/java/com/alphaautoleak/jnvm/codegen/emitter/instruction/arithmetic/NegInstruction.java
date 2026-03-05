package com.alphaautoleak.jnvm.codegen.emitter.instruction.arithmetic;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * Negation instruction (INEG, LNEG, FNEG, DNEG)
 */
public class NegInstruction extends Instruction {
    private final String type;
    private final String vmType;

    public NegInstruction(int opcode, String name, String type) {
        super(opcode, name);
        this.type = type;
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
        w.println("                frame.stack[frame.sp-1]." + type + " = -frame.stack[frame.sp-1]." + type + ";");
        w.println("                frame.stackTypes[frame.sp-1] = " + vmType + ";");
        pcIncBreak(w);
    }
}