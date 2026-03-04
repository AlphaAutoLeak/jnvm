package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * INVOKEINTERFACE 指令
 */
public class InvokeInterfaceInstruction extends Instruction {
    public InvokeInterfaceInstruction() {
        super(0xb9, "INVOKEINTERFACE");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        InvokeHelper.generate(w, false);
    }
}
