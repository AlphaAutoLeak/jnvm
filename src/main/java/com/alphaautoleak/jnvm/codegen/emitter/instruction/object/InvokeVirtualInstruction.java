package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * INVOKEVIRTUAL instruction
 */
public class InvokeVirtualInstruction extends Instruction {
    public InvokeVirtualInstruction() {
        super(0xb6, "INVOKEVIRTUAL");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        InvokeHelper.generate(w, false);
    }

    @Override
    public void generateComputedGoto(PrintWriter w) {
        InvokeHelper.generateComputedGoto(w, false, opcode, comment);
    }
}