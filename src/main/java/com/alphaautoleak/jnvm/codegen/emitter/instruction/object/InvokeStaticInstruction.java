package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * INVOKESTATIC 指令
 */
public class InvokeStaticInstruction extends Instruction {
    public InvokeStaticInstruction() {
        super(0xb8, "INVOKESTATIC");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        InvokeHelper.generate(w, true);
    }
}
