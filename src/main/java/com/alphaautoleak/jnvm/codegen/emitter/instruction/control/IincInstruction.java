package com.alphaautoleak.jnvm.codegen.emitter.instruction.control;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * IINC instruction
 */
public class IincInstruction extends Instruction {
    public IincInstruction() {
        super(0x84, "IINC");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                frame.locals[meta->iincIndex].i += meta->iincConst;");
        pcIncBreak(w);
    }
}