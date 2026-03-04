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

    public CastInstruction(int opcode, String name, String fromType, String toType, String castType) {
        super(opcode, name);
        this.fromType = fromType;
        this.toType = toType;
        this.castType = castType;
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                frame.stack[frame.sp-1]." + toType + " = (" + castType + ")frame.stack[frame.sp-1]." + fromType + ";");
        pcIncBreak(w);
    }
}