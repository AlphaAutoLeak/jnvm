package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * INVOKEINTERFACE instruction
 */
public class InvokeInterfaceInstruction extends Instruction {
    public InvokeInterfaceInstruction() {
        super(0xb9, "INVOKEINTERFACE");
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