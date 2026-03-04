package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * INVOKESPECIAL 指令
 */
public class InvokeSpecialInstruction extends Instruction {
    public InvokeSpecialInstruction() {
        super(0xb7, "INVOKESPECIAL");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        InvokeHelper.generate(w, false);
    }
}
