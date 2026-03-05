package com.alphaautoleak.jnvm.codegen.emitter.instruction.arithmetic;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * Shift instruction (ISHL, LSHL, ISHR, LSHR, IUSHR, LUSHR)
 */
public class ShiftInstruction extends Instruction {
    private final String type;
    private final String op;
    private final boolean unsigned;
    private final String vmType;

    public ShiftInstruction(int opcode, String name, String type, String op, boolean unsigned) {
        super(opcode, name);
        this.type = type;
        this.op = op;
        this.unsigned = unsigned;
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
        if (unsigned) {
            String unsignedType = type.equals("i") ? "uint32_t" : "uint64_t";
            w.println("                frame.stack[frame.sp-2]." + type + " = (" + unsignedType + ")frame.stack[frame.sp-2]." + type + " " + op + " frame.stack[frame.sp-1].i;");
        } else {
            w.println("                frame.stack[frame.sp-2]." + type + " " + op + "= frame.stack[frame.sp-1].i;");
        }
        w.println("                frame.stackTypes[frame.sp-2] = " + vmType + ";");
        w.println("                frame.sp--;");
        pcIncBreak(w);
    }
}