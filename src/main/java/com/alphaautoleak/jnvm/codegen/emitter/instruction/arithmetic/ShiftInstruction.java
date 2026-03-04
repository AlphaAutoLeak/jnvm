package com.alphaautoleak.jnvm.codegen.emitter.instruction.arithmetic;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * 移位指令 (ISHL, LSHL, ISHR, LSHR, IUSHR, LUSHR)
 */
public class ShiftInstruction extends Instruction {
    private final String type;
    private final String op;
    private final boolean unsigned;

    public ShiftInstruction(int opcode, String name, String type, String op, boolean unsigned) {
        super(opcode, name);
        this.type = type;
        this.op = op;
        this.unsigned = unsigned;
    }

    @Override
    protected void generateBody(PrintWriter w) {
        if (unsigned) {
            String unsignedType = type.equals("i") ? "uint32_t" : "uint64_t";
            w.println("                frame.stack[frame.sp-2]." + type + " = (" + unsignedType + ")frame.stack[frame.sp-2]." + type + " " + op + " frame.stack[frame.sp-1].i;");
        } else {
            w.println("                frame.stack[frame.sp-2]." + type + " " + op + "= frame.stack[frame.sp-1].i;");
        }
        w.println("                frame.sp--;");
        pcIncBreak(w);
    }
}
