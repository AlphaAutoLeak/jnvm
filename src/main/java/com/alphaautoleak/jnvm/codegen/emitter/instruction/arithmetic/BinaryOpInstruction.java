package com.alphaautoleak.jnvm.codegen.emitter.instruction.arithmetic;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * Binary operation instruction (IADD, LADD, FADD, DADD, ISUB, etc.) - 64-bit only
 */
public class BinaryOpInstruction extends Instruction {
    private final String type;
    private final String op;

    public BinaryOpInstruction(int opcode, String name, String type, String op) {
        super(opcode, name);
        this.type = type;
        this.op = op;
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                frame.stack[frame.sp-2]." + type + " " + op + "= frame.stack[frame.sp-1]." + type + ";");
        w.println("                frame.sp--;");
        pcIncBreak(w);
    }
}
