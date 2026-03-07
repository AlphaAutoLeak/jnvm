package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * INVOKESPECIAL instruction
 */
public class InvokeSpecialInstruction extends Instruction {
    public InvokeSpecialInstruction() {
        super(0xb7, "INVOKESPECIAL");
    }
    
    @Override
    public boolean needsMeta() {
        return true;
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