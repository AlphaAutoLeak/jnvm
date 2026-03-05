package com.alphaautoleak.jnvm.codegen.emitter.instruction.arithmetic;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * Type cast instruction (I2L, I2F, I2D, L2I, etc.)
 */
public class CastInstruction extends Instruction {
    private final String fromType;
    private final String toType;
    private final String castType;
    private final String vmType;

    public CastInstruction(int opcode, String name, String fromType, String toType, String castType) {
        super(opcode, name);
        this.fromType = fromType;
        this.toType = toType;
        this.castType = castType;
        this.vmType = getVmType(toType);
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
        w.println("                frame.stack[frame.sp-1]." + toType + " = (" + castType + ")frame.stack[frame.sp-1]." + fromType + ";");
        w.println("                frame.stackTypes[frame.sp-1] = " + vmType + ";");
        pcIncBreak(w);
    }
}