package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * INVOKEVIRTUAL 指令
 */
public class InvokeVirtualInstruction extends Instruction {
    public InvokeVirtualInstruction() {
        super(0xb6, "INVOKEVIRTUAL");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        InvokeHelper.generate(w, false);
    }
}
